package com.eventdrivencommerce.product.application.service;

import com.eventdrivencommerce.product.application.event.ProductChangedV1;
import com.eventdrivencommerce.product.application.exception.ConcurrentProductModificationException;
import com.eventdrivencommerce.product.application.exception.DuplicateSkuException;
import com.eventdrivencommerce.product.application.exception.ProductNotFoundException;
import com.eventdrivencommerce.product.application.model.CreateProductCommand;
import com.eventdrivencommerce.product.application.model.DeactivateProductCommand;
import com.eventdrivencommerce.product.application.model.PageResult;
import com.eventdrivencommerce.product.application.model.ProductResult;
import com.eventdrivencommerce.product.application.model.ProductSearchQuery;
import com.eventdrivencommerce.product.application.model.UpdateProductCommand;
import com.eventdrivencommerce.product.application.port.in.ProductCommandUseCase;
import com.eventdrivencommerce.product.application.port.in.ProductQueryUseCase;
import com.eventdrivencommerce.product.application.port.out.ProductEventOutbox;
import com.eventdrivencommerce.product.application.port.out.ProductRepository;
import com.eventdrivencommerce.product.domain.model.Money;
import com.eventdrivencommerce.product.domain.model.Product;
import com.eventdrivencommerce.product.domain.model.Sku;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ProductApplicationService implements ProductCommandUseCase, ProductQueryUseCase {

    private final ProductRepository repository;
    private final ProductEventOutbox outbox;
    private final Clock clock;

    public ProductApplicationService(ProductRepository repository, ProductEventOutbox outbox, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResult create(CreateProductCommand command) {
        Sku sku = new Sku(command.sku());
        if (repository.existsBySku(sku)) {
            throw new DuplicateSkuException(sku.value());
        }
        Instant now = clock.instant();
        Product product = Product.create(
                UUID.randomUUID(), sku, command.name(), command.description(), command.category(),
                Money.of(command.price(), command.currency()), now
        );
        Product saved = save(product);
        outbox.append(ProductChangedV1.from(saved, ProductChangedV1.ChangeType.CREATED,
                requiredId(command.correlationId()), requiredId(command.causationId()), now));
        return ProductResult.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResult update(UpdateProductCommand command) {
        Product product = find(command.productId());
        assertVersion(product, command.expectedVersion());
        Sku sku = new Sku(command.sku());
        if (repository.existsBySkuAndIdNot(sku, product.id())) {
            throw new DuplicateSkuException(sku.value());
        }
        Instant now = clock.instant();
        product.update(sku, command.name(), command.description(), command.category(),
                Money.of(command.price(), command.currency()), now);
        Product saved = save(product);
        outbox.append(ProductChangedV1.from(saved, ProductChangedV1.ChangeType.UPDATED,
                requiredId(command.correlationId()), requiredId(command.causationId()), now));
        return ProductResult.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResult deactivate(DeactivateProductCommand command) {
        Product product = find(command.productId());
        assertVersion(product, command.expectedVersion());
        Instant now = clock.instant();
        if (!product.deactivate(now)) {
            return ProductResult.from(product);
        }
        Product saved = save(product);
        outbox.append(ProductChangedV1.from(saved, ProductChangedV1.ChangeType.DEACTIVATED,
                requiredId(command.correlationId()), requiredId(command.causationId()), now));
        return ProductResult.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products", key = "#productId")
    public ProductResult get(UUID productId) {
        return ProductResult.from(find(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductResult> search(ProductSearchQuery query) {
        PageResult<Product> page = repository.search(query);
        return new PageResult<>(page.items().stream().map(ProductResult::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages(), page.first(), page.last());
    }

    private Product find(UUID productId) {
        return repository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private Product save(Product product) {
        try {
            return repository.save(product);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConcurrentProductModificationException(product.id());
        }
    }

    private static void assertVersion(Product product, long expectedVersion) {
        if (product.version() != expectedVersion) {
            throw new ConcurrentProductModificationException(product.id());
        }
    }

    private static UUID requiredId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("Correlation and causation IDs are required");
        }
        return value;
    }
}

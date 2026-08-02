package com.eventdrivencommerce.product.infrastructure.persistence;

import com.eventdrivencommerce.product.application.exception.DuplicateSkuException;
import com.eventdrivencommerce.product.application.model.PageResult;
import com.eventdrivencommerce.product.application.model.ProductSearchQuery;
import com.eventdrivencommerce.product.application.port.out.ProductRepository;
import com.eventdrivencommerce.product.domain.model.Product;
import com.eventdrivencommerce.product.domain.model.Sku;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ProductPersistenceAdapter implements ProductRepository {

    private final SpringDataProductRepository repository;

    ProductPersistenceAdapter(SpringDataProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = repository.findById(product.id())
                .orElseGet(() -> ProductJpaEntity.create(product.id()));
        ProductPersistenceMapper.copyToEntity(product, entity);
        try {
            return ProductPersistenceMapper.toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSkuException(product.sku().value());
        }
    }

    @Override
    public Optional<Product> findById(UUID productId) {
        return repository.findById(productId).map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsBySku(Sku sku) {
        return repository.existsBySku(sku.value());
    }

    @Override
    public boolean existsBySkuAndIdNot(Sku sku, UUID productId) {
        return repository.existsBySkuAndIdNot(sku.value(), productId);
    }

    @Override
    public PageResult<Product> search(ProductSearchQuery query) {
        Sort.Direction direction = query.sortDirection() == ProductSearchQuery.SortDirection.ASC
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest request = PageRequest.of(query.page(), query.size(),
                Sort.by(direction, query.sortField().property()).and(Sort.by("id")));
        Page<ProductJpaEntity> page = repository.findAll(specification(query), request);
        return new PageResult<>(page.stream().map(ProductPersistenceMapper::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast());
    }

    private static Specification<ProductJpaEntity> specification(ProductSearchQuery query) {
        List<Specification<ProductJpaEntity>> filters = new ArrayList<>();
        if (query.name() != null) {
            String pattern = "%" + escapeLike(query.name().toLowerCase()) + "%";
            filters.add((root, criteria, builder) -> builder.like(builder.lower(root.get("name")), pattern, '\\'));
        }
        if (query.category() != null) {
            filters.add((root, criteria, builder) ->
                    builder.equal(builder.lower(root.get("category")), query.category().toLowerCase()));
        }
        if (query.active() != null) {
            filters.add((root, criteria, builder) -> builder.equal(root.get("active"), query.active()));
        }
        return filters.stream().reduce(Specification.allOf(), Specification::and);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

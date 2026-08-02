package com.eventdrivencommerce.product.application.port.out;

import com.eventdrivencommerce.product.application.model.PageResult;
import com.eventdrivencommerce.product.application.model.ProductSearchQuery;
import com.eventdrivencommerce.product.domain.model.Product;
import com.eventdrivencommerce.product.domain.model.Sku;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID productId);
    boolean existsBySku(Sku sku);
    boolean existsBySkuAndIdNot(Sku sku, UUID productId);
    PageResult<Product> search(ProductSearchQuery query);
}

package com.eventdrivencommerce.product.infrastructure.persistence;

import com.eventdrivencommerce.product.domain.model.Money;
import com.eventdrivencommerce.product.domain.model.Product;
import com.eventdrivencommerce.product.domain.model.Sku;

final class ProductPersistenceMapper {

    private ProductPersistenceMapper() {}

    static Product toDomain(ProductJpaEntity entity) {
        return Product.rehydrate(
                entity.getId(), new Sku(entity.getSku()), entity.getName(), entity.getDescription(),
                entity.getCategory(), Money.of(entity.getPrice(), entity.getCurrency()), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion()
        );
    }

    static void copyToEntity(Product product, ProductJpaEntity entity) {
        entity.setSku(product.sku().value());
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setCategory(product.category());
        entity.setPrice(product.price().amount());
        entity.setCurrency(product.price().currency().getCurrencyCode());
        entity.setActive(product.active());
        entity.setCreatedAt(product.createdAt());
        entity.setUpdatedAt(product.updatedAt());
    }
}

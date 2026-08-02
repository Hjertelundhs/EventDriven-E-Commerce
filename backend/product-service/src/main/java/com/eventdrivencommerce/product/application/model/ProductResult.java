package com.eventdrivencommerce.product.application.model;

import com.eventdrivencommerce.product.domain.model.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResult(
        UUID id,
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static ProductResult from(Product product) {
        return new ProductResult(
                product.id(), product.sku().value(), product.name(), product.description(), product.category(),
                product.price().amount(), product.price().currency().getCurrencyCode(), product.active(),
                product.createdAt(), product.updatedAt(), product.version()
        );
    }
}

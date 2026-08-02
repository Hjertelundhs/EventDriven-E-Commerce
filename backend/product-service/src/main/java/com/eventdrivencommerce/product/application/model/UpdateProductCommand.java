package com.eventdrivencommerce.product.application.model;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductCommand(
        UUID productId,
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency,
        long expectedVersion,
        UUID correlationId,
        UUID causationId
) {}

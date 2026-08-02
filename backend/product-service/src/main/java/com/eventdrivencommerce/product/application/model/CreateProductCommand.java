package com.eventdrivencommerce.product.application.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductCommand(
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency,
        UUID correlationId,
        UUID causationId
) {}

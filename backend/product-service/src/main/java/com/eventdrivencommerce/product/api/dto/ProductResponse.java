package com.eventdrivencommerce.product.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        String category,
        @Schema(type = "string", example = "129.90") BigDecimal price,
        @Schema(example = "SEK") String currency,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}

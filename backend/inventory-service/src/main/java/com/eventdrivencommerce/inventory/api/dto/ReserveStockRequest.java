package com.eventdrivencommerce.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record ReserveStockRequest(
        @NotNull UUID orderId,
        @NotBlank @Pattern(regexp = "(?i)[A-Z0-9][A-Z0-9._-]{2,63}") String sku,
        @Positive int quantity,
        @PositiveOrZero long inventoryVersion) {}

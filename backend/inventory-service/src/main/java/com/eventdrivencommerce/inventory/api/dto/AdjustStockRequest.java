package com.eventdrivencommerce.inventory.api.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record AdjustStockRequest(
        @PositiveOrZero int totalQuantity,
        @PositiveOrZero long version) {}

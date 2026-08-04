package com.eventdrivencommerce.inventory.api.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ReceiveStockRequest(
        @Positive int quantity,
        @PositiveOrZero long version) {}

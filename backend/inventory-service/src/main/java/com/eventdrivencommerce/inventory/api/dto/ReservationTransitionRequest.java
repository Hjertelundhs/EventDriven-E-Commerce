package com.eventdrivencommerce.inventory.api.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record ReservationTransitionRequest(
        @PositiveOrZero long reservationVersion,
        @PositiveOrZero long inventoryVersion) {}

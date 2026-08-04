package com.eventdrivencommerce.inventory.api.dto;

import com.eventdrivencommerce.inventory.domain.model.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID orderId,
        String sku,
        int quantity,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version,
        long inventoryVersion) {}

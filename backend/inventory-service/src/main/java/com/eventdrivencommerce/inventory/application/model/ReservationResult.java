package com.eventdrivencommerce.inventory.application.model;

import com.eventdrivencommerce.inventory.domain.model.InventoryReservation;
import com.eventdrivencommerce.inventory.domain.model.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResult(
        UUID id,
        UUID orderId,
        String sku,
        int quantity,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version,
        long inventoryVersion) {

    public static ReservationResult from(InventoryReservation reservation, long inventoryVersion) {
        return new ReservationResult(reservation.id(), reservation.orderId(), reservation.sku().value(),
                reservation.quantity(), reservation.status(), reservation.createdAt(), reservation.updatedAt(),
                reservation.version(), inventoryVersion);
    }
}

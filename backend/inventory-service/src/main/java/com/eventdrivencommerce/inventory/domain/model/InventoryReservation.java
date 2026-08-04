package com.eventdrivencommerce.inventory.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class InventoryReservation {

    private final UUID id;
    private final UUID inventoryItemId;
    private final UUID orderId;
    private final Sku sku;
    private final int quantity;
    private final Instant createdAt;
    private ReservationStatus status;
    private Instant updatedAt;
    private long version;

    private InventoryReservation(
            UUID id,
            UUID inventoryItemId,
            UUID orderId,
            Sku sku,
            int quantity,
            ReservationStatus status,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = Objects.requireNonNull(id, "Reservation ID is required");
        this.inventoryItemId = Objects.requireNonNull(inventoryItemId, "Inventory item ID is required");
        this.orderId = Objects.requireNonNull(orderId, "Order ID is required");
        this.sku = Objects.requireNonNull(sku, "SKU is required");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }
        this.quantity = quantity;
        this.status = Objects.requireNonNull(status, "Reservation status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated timestamp is required");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Updated timestamp cannot precede creation");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Version cannot be negative");
        }
        this.version = version;
    }

    public static InventoryReservation create(
            UUID id, UUID inventoryItemId, UUID orderId, Sku sku, int quantity, Instant now) {
        return new InventoryReservation(id, inventoryItemId, orderId, sku, quantity,
                ReservationStatus.RESERVED, now, now, 0);
    }

    public static InventoryReservation rehydrate(
            UUID id,
            UUID inventoryItemId,
            UUID orderId,
            Sku sku,
            int quantity,
            ReservationStatus status,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        return new InventoryReservation(id, inventoryItemId, orderId, sku, quantity,
                status, createdAt, updatedAt, version);
    }

    public boolean release(Instant now) {
        validateUpdateTime(now);
        if (status == ReservationStatus.RELEASED) {
            return false;
        }
        if (status == ReservationStatus.COMPLETED) {
            throw new IllegalStateException("A completed reservation cannot be released");
        }
        status = ReservationStatus.RELEASED;
        updatedAt = now;
        return true;
    }

    public boolean complete(Instant now) {
        validateUpdateTime(now);
        if (status == ReservationStatus.COMPLETED) {
            return false;
        }
        if (status == ReservationStatus.RELEASED) {
            throw new IllegalStateException("A released reservation cannot be completed");
        }
        status = ReservationStatus.COMPLETED;
        updatedAt = now;
        return true;
    }

    private void validateUpdateTime(Instant now) {
        Objects.requireNonNull(now, "Updated timestamp is required");
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("Updated timestamp cannot precede creation");
        }
    }

    public UUID id() { return id; }
    public UUID inventoryItemId() { return inventoryItemId; }
    public UUID orderId() { return orderId; }
    public Sku sku() { return sku; }
    public int quantity() { return quantity; }
    public ReservationStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}

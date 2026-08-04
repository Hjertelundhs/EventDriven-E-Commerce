package com.eventdrivencommerce.inventory.domain.model;

import com.eventdrivencommerce.inventory.domain.exception.InsufficientStockException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class InventoryItem {

    private final UUID id;
    private final Sku sku;
    private final Instant createdAt;
    private int availableQuantity;
    private int reservedQuantity;
    private int totalQuantity;
    private Instant updatedAt;
    private long version;

    private InventoryItem(
            UUID id,
            Sku sku,
            int availableQuantity,
            int reservedQuantity,
            int totalQuantity,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = Objects.requireNonNull(id, "Inventory ID is required");
        this.sku = Objects.requireNonNull(sku, "SKU is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated timestamp is required");
        if (version < 0) {
            throw new IllegalArgumentException("Version cannot be negative");
        }
        validateQuantities(availableQuantity, reservedQuantity, totalQuantity);
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.totalQuantity = totalQuantity;
        this.version = version;
    }

    public static InventoryItem create(UUID id, Sku sku, Instant now) {
        return new InventoryItem(id, sku, 0, 0, 0, now, now, 0);
    }

    public static InventoryItem rehydrate(
            UUID id,
            Sku sku,
            int availableQuantity,
            int reservedQuantity,
            int totalQuantity,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        return new InventoryItem(id, sku, availableQuantity, reservedQuantity, totalQuantity, createdAt, updatedAt, version);
    }

    public void receive(int quantity, Instant now) {
        requirePositive(quantity);
        validateUpdateTime(now);
        int newAvailableQuantity = Math.addExact(availableQuantity, quantity);
        int newTotalQuantity = Math.addExact(totalQuantity, quantity);
        availableQuantity = newAvailableQuantity;
        totalQuantity = newTotalQuantity;
        changed(now);
    }

    public void adjustTotal(int newTotalQuantity, Instant now) {
        validateUpdateTime(now);
        if (newTotalQuantity < reservedQuantity) {
            throw new IllegalArgumentException("Total quantity cannot be lower than reserved quantity");
        }
        totalQuantity = newTotalQuantity;
        availableQuantity = newTotalQuantity - reservedQuantity;
        changed(now);
    }

    public void reserve(int quantity, Instant now) {
        requirePositive(quantity);
        validateUpdateTime(now);
        if (quantity > availableQuantity) {
            throw new InsufficientStockException(sku.value(), quantity, availableQuantity);
        }
        int newReservedQuantity = Math.addExact(reservedQuantity, quantity);
        availableQuantity -= quantity;
        reservedQuantity = newReservedQuantity;
        changed(now);
    }

    public void release(int quantity, Instant now) {
        requireReservedQuantity(quantity);
        validateUpdateTime(now);
        int newAvailableQuantity = Math.addExact(availableQuantity, quantity);
        reservedQuantity -= quantity;
        availableQuantity = newAvailableQuantity;
        changed(now);
    }

    public void complete(int quantity, Instant now) {
        requireReservedQuantity(quantity);
        validateUpdateTime(now);
        reservedQuantity -= quantity;
        totalQuantity -= quantity;
        changed(now);
    }

    private void requireReservedQuantity(int quantity) {
        requirePositive(quantity);
        if (quantity > reservedQuantity) {
            throw new IllegalArgumentException("Quantity cannot exceed reserved stock");
        }
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    private static void validateQuantities(int available, int reserved, int total) {
        if (available < 0 || reserved < 0 || total < 0) {
            throw new IllegalArgumentException("Inventory quantities cannot be negative");
        }
        if ((long) available + reserved != total) {
            throw new IllegalArgumentException("Total quantity must equal available plus reserved quantity");
        }
    }

    private void validateUpdateTime(Instant now) {
        Objects.requireNonNull(now, "Updated timestamp is required");
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("Updated timestamp cannot precede creation");
        }
    }

    private void changed(Instant now) {
        updatedAt = now;
    }

    public UUID id() { return id; }
    public Sku sku() { return sku; }
    public int availableQuantity() { return availableQuantity; }
    public int reservedQuantity() { return reservedQuantity; }
    public int totalQuantity() { return totalQuantity; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}

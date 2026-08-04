package com.eventdrivencommerce.inventory.infrastructure.persistence;

import com.eventdrivencommerce.inventory.domain.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_reservation_order_sku",
                columnNames = {"order_id", "sku"}))
class InventoryReservationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected InventoryReservationJpaEntity() {}

    static InventoryReservationJpaEntity create(UUID id) {
        InventoryReservationJpaEntity entity = new InventoryReservationJpaEntity();
        entity.id = id;
        return entity;
    }

    UUID getId() { return id; }
    UUID getInventoryItemId() { return inventoryItemId; }
    UUID getOrderId() { return orderId; }
    String getSku() { return sku; }
    int getQuantity() { return quantity; }
    ReservationStatus getStatus() { return status; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    long getVersion() { return version == null ? 0 : version; }

    void setInventoryItemId(UUID inventoryItemId) { this.inventoryItemId = inventoryItemId; }
    void setOrderId(UUID orderId) { this.orderId = orderId; }
    void setSku(String sku) { this.sku = sku; }
    void setQuantity(int quantity) { this.quantity = quantity; }
    void setStatus(ReservationStatus status) { this.status = status; }
    void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

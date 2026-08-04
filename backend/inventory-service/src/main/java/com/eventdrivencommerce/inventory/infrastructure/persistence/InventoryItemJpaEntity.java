package com.eventdrivencommerce.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_items_sku", columnNames = "sku"))
class InventoryItemJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected InventoryItemJpaEntity() {}

    static InventoryItemJpaEntity create(UUID id) {
        InventoryItemJpaEntity entity = new InventoryItemJpaEntity();
        entity.id = id;
        return entity;
    }

    UUID getId() { return id; }
    String getSku() { return sku; }
    int getAvailableQuantity() { return availableQuantity; }
    int getReservedQuantity() { return reservedQuantity; }
    int getTotalQuantity() { return totalQuantity; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    long getVersion() { return version == null ? 0 : version; }

    void setSku(String sku) { this.sku = sku; }
    void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
    void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
    void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

package com.eventdrivencommerce.product.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"))
class ProductJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 120)
    private String category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected ProductJpaEntity() {}

    static ProductJpaEntity create(UUID id) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.id = id;
        return entity;
    }

    UUID getId() { return id; }
    String getSku() { return sku; }
    String getName() { return name; }
    String getDescription() { return description; }
    String getCategory() { return category; }
    BigDecimal getPrice() { return price; }
    String getCurrency() { return currency; }
    boolean isActive() { return active; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    long getVersion() { return version == null ? 0 : version; }

    void setSku(String sku) { this.sku = sku; }
    void setName(String name) { this.name = name; }
    void setDescription(String description) { this.description = description; }
    void setCategory(String category) { this.category = category; }
    void setPrice(BigDecimal price) { this.price = price; }
    void setCurrency(String currency) { this.currency = currency; }
    void setActive(boolean active) { this.active = active; }
    void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

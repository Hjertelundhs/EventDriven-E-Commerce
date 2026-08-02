package com.eventdrivencommerce.product.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Product {

    private final UUID id;
    private Sku sku;
    private String name;
    private String description;
    private String category;
    private Money price;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    private Product(UUID id, Sku sku, String name, String description, String category, Money price,
                    boolean active, Instant createdAt, Instant updatedAt, long version) {
        this.id = Objects.requireNonNull(id, "Product ID is required");
        this.sku = Objects.requireNonNull(sku, "SKU is required");
        this.name = requiredText(name, "Name", 160);
        this.description = optionalText(description, "Description", 4000);
        this.category = requiredText(category, "Category", 120);
        this.price = Objects.requireNonNull(price, "Price is required");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "Created time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time is required");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Updated time cannot precede created time");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Version cannot be negative");
        }
        this.version = version;
    }

    public static Product create(UUID id, Sku sku, String name, String description, String category,
                                 Money price, Instant now) {
        return new Product(id, sku, name, description, category, price, true, now, now, 0);
    }

    public static Product rehydrate(UUID id, Sku sku, String name, String description, String category,
                                    Money price, boolean active, Instant createdAt, Instant updatedAt, long version) {
        return new Product(id, sku, name, description, category, price, active, createdAt, updatedAt, version);
    }

    public void update(Sku newSku, String newName, String newDescription, String newCategory,
                       Money newPrice, Instant now) {
        if (!active) {
            throw new IllegalStateException("An inactive product cannot be updated");
        }
        sku = Objects.requireNonNull(newSku, "SKU is required");
        name = requiredText(newName, "Name", 160);
        description = optionalText(newDescription, "Description", 4000);
        category = requiredText(newCategory, "Category", 120);
        price = Objects.requireNonNull(newPrice, "Price is required");
        touch(now);
    }

    public boolean deactivate(Instant now) {
        if (!active) {
            return false;
        }
        active = false;
        touch(now);
        return true;
    }

    private void touch(Instant now) {
        Objects.requireNonNull(now, "Updated time is required");
        if (now.isBefore(updatedAt)) {
            throw new IllegalArgumentException("Updated time cannot move backwards");
        }
        updatedAt = now;
    }

    private static String requiredText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.strip();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(field + " exceeds " + maximumLength + " characters");
        }
        return normalized;
    }

    private static String optionalText(String value, String field, int maximumLength) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(field + " exceeds " + maximumLength + " characters");
        }
        return normalized;
    }

    public UUID id() { return id; }
    public Sku sku() { return sku; }
    public String name() { return name; }
    public String description() { return description; }
    public String category() { return category; }
    public Money price() { return price; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
}

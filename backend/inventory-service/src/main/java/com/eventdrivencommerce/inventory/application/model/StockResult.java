package com.eventdrivencommerce.inventory.application.model;

import com.eventdrivencommerce.inventory.domain.model.InventoryItem;

import java.time.Instant;
import java.util.UUID;

public record StockResult(
        UUID id,
        String sku,
        int availableQuantity,
        int reservedQuantity,
        int totalQuantity,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public static StockResult from(InventoryItem item) {
        return new StockResult(item.id(), item.sku().value(), item.availableQuantity(), item.reservedQuantity(),
                item.totalQuantity(), item.createdAt(), item.updatedAt(), item.version());
    }
}

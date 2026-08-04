package com.eventdrivencommerce.inventory.infrastructure.persistence;

import com.eventdrivencommerce.inventory.domain.model.InventoryItem;
import com.eventdrivencommerce.inventory.domain.model.Sku;

final class InventoryPersistenceMapper {

    private InventoryPersistenceMapper() {}

    static InventoryItem toDomain(InventoryItemJpaEntity entity) {
        return InventoryItem.rehydrate(entity.getId(), new Sku(entity.getSku()), entity.getAvailableQuantity(),
                entity.getReservedQuantity(), entity.getTotalQuantity(), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getVersion());
    }

    static void copyToEntity(InventoryItem item, InventoryItemJpaEntity entity) {
        entity.setSku(item.sku().value());
        entity.setAvailableQuantity(item.availableQuantity());
        entity.setReservedQuantity(item.reservedQuantity());
        entity.setTotalQuantity(item.totalQuantity());
        entity.setCreatedAt(item.createdAt());
        entity.setUpdatedAt(item.updatedAt());
    }
}

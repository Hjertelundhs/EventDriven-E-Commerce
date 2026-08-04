package com.eventdrivencommerce.inventory.application.port.out;

import com.eventdrivencommerce.inventory.domain.model.InventoryItem;
import com.eventdrivencommerce.inventory.domain.model.Sku;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

    InventoryItem save(InventoryItem item);

    Optional<InventoryItem> findById(UUID inventoryItemId);

    Optional<InventoryItem> findBySku(Sku sku);
}

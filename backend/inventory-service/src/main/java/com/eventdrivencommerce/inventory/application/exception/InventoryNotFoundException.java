package com.eventdrivencommerce.inventory.application.exception;

public final class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(String sku) {
        super("Inventory item with SKU %s was not found".formatted(sku));
    }
}

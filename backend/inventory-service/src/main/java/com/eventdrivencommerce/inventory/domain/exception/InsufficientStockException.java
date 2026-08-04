package com.eventdrivencommerce.inventory.domain.exception;

public final class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, int requestedQuantity, int availableQuantity) {
        super("Insufficient stock for SKU %s: requested %d, available %d"
                .formatted(sku, requestedQuantity, availableQuantity));
    }
}

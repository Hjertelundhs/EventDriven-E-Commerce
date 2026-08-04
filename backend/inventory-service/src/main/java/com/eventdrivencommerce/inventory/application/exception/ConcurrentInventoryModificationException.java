package com.eventdrivencommerce.inventory.application.exception;

public final class ConcurrentInventoryModificationException extends RuntimeException {

    public ConcurrentInventoryModificationException(String resource) {
        super("Inventory resource %s was modified concurrently".formatted(resource));
    }
}

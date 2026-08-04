package com.eventdrivencommerce.inventory.application.exception;

import java.util.UUID;

public final class DuplicateReservationException extends RuntimeException {

    public DuplicateReservationException(UUID orderId, String sku) {
        super("Order %s already has a reservation with different data for SKU %s".formatted(orderId, sku));
    }
}

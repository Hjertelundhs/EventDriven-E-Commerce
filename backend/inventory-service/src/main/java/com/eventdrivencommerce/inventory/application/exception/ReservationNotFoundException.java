package com.eventdrivencommerce.inventory.application.exception;

import java.util.UUID;

public final class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID reservationId) {
        super("Inventory reservation %s was not found".formatted(reservationId));
    }
}

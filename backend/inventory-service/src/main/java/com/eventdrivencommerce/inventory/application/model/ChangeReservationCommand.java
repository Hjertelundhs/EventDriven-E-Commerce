package com.eventdrivencommerce.inventory.application.model;

import java.util.UUID;

public record ChangeReservationCommand(
        UUID reservationId, long expectedReservationVersion, long expectedInventoryVersion) {}

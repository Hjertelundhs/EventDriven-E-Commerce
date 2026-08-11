package com.eventdrivencommerce.inventory.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InventoryEventOutbox {
    void reserved(UUID orderId, List<Item> items, UUID correlationId, UUID causationId, Instant now);
    void reservationFailed(UUID orderId, List<Failure> failures, UUID correlationId, UUID causationId, Instant now);

    record Item(String sku, int quantity) {}
    record Failure(String sku, int requestedQuantity, int availableQuantity, String reasonCode) {}
}

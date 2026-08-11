package com.eventdrivencommerce.inventory.application.model;

import java.util.List;
import java.util.UUID;

public record OrderInventoryRequest(UUID eventId, UUID orderId, UUID correlationId, List<Line> lines) {
    public record Line(String sku, int quantity) {}
}

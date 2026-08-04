package com.eventdrivencommerce.inventory.api.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        String sku,
        int availableQuantity,
        int reservedQuantity,
        int totalQuantity,
        Instant createdAt,
        Instant updatedAt,
        long version) {}

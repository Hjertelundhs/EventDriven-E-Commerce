package com.eventdrivencommerce.inventory.application.model;

import java.util.UUID;

public record ReserveStockCommand(UUID orderId, String sku, int quantity, long expectedInventoryVersion) {}

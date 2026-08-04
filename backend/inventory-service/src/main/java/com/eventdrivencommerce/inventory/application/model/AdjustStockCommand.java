package com.eventdrivencommerce.inventory.application.model;

public record AdjustStockCommand(String sku, int totalQuantity, long expectedVersion) {}

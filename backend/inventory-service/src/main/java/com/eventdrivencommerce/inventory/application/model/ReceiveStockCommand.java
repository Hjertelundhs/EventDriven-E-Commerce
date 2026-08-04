package com.eventdrivencommerce.inventory.application.model;

public record ReceiveStockCommand(String sku, int quantity, long expectedVersion) {}

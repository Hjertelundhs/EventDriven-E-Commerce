package com.eventdrivencommerce.order.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record OrderLine(UUID productId, String sku, String productName, int quantity,
                        BigDecimal unitPrice, BigDecimal totalPrice) {
    public OrderLine {
        if (productId == null) throw new IllegalArgumentException("productId is required");
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("sku is required");
        if (productName == null || productName.isBlank()) throw new IllegalArgumentException("productName is required");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be positive");
        unitPrice = money(unitPrice);
        if (unitPrice.signum() < 0) throw new IllegalArgumentException("unitPrice cannot be negative");
        BigDecimal calculated = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        totalPrice = totalPrice == null ? calculated : money(totalPrice);
        if (totalPrice.compareTo(calculated) != 0) throw new IllegalArgumentException("line total does not match quantity and unit price");
    }
    public static OrderLine priced(UUID productId, String sku, String name, int quantity, BigDecimal price) {
        return new OrderLine(productId, sku, name, quantity, price, null);
    }
    static BigDecimal money(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("money value is required");
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

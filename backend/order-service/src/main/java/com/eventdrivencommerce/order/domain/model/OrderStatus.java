package com.eventdrivencommerce.order.domain.model;

public enum OrderStatus {
    PENDING, INVENTORY_RESERVED, PAYMENT_PENDING, PAID, DELIVERY_CREATED, COMPLETED,
    CANCELLED, PAYMENT_FAILED, DELIVERY_FAILED;

    public boolean terminal() { return this == COMPLETED || this == CANCELLED; }
}

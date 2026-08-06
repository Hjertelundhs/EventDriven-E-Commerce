package com.eventdrivencommerce.order.application.model;

import com.eventdrivencommerce.order.domain.model.Address;
import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(UUID customerId, List<RequestedLine> lines, String currency,
                                 Address shippingAddress, Address billingAddress,
                                 String idempotencyKey, String requestFingerprint, UUID correlationId) {
    public record RequestedLine(UUID productId, int quantity) {}
}

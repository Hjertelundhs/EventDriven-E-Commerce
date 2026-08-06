package com.eventdrivencommerce.order.application.port.out;

import com.eventdrivencommerce.order.domain.model.Order;
import java.time.Instant;
import java.util.UUID;

public interface OrderEventOutbox {
    void orderCreated(Order order, UUID correlationId, UUID causationId, Instant now);
    void orderCompleted(Order order, UUID correlationId, UUID causationId, Instant now);
    void orderCancelled(Order order, UUID correlationId, UUID causationId, Instant now);
}

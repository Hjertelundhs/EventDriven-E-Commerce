package com.eventdrivencommerce.order.domain.exception;

import com.eventdrivencommerce.order.domain.model.OrderStatus;

public class InvalidOrderTransitionException extends RuntimeException {
    public InvalidOrderTransitionException(OrderStatus current, String event) {
        super("Cannot apply " + event + " while order is " + current);
    }
}

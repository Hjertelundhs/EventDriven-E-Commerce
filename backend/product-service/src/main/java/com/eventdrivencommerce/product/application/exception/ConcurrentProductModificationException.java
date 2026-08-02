package com.eventdrivencommerce.product.application.exception;

import java.util.UUID;

public final class ConcurrentProductModificationException extends RuntimeException {
    public ConcurrentProductModificationException(UUID productId) {
        super("Product " + productId + " was changed by another request");
    }
}

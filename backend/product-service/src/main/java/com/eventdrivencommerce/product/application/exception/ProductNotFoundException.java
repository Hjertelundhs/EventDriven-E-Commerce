package com.eventdrivencommerce.product.application.exception;

import java.util.UUID;

public final class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID productId) {
        super("Product " + productId + " was not found");
    }
}

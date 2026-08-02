package com.eventdrivencommerce.product.application.exception;

public final class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String sku) {
        super("SKU " + sku + " is already assigned to another product");
    }
}

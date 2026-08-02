package com.eventdrivencommerce.product.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record Sku(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9][A-Z0-9._-]{2,63}");

    public Sku {
        Objects.requireNonNull(value, "SKU is required");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("SKU must be 3-64 characters using letters, digits, dot, underscore or hyphen");
        }
    }
}

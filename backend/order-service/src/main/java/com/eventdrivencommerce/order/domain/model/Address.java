package com.eventdrivencommerce.order.domain.model;

import java.util.Objects;

public record Address(String recipient, String line1, String line2, String postalCode, String city, String countryCode) {
    public Address {
        recipient = required(recipient, "recipient"); line1 = required(line1, "line1");
        postalCode = required(postalCode, "postalCode"); city = required(city, "city");
        countryCode = required(countryCode, "countryCode").toUpperCase();
        line2 = Objects.requireNonNullElse(line2, "").trim();
        if (countryCode.length() != 2) throw new IllegalArgumentException("countryCode must be ISO 3166-1 alpha-2");
    }
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
}

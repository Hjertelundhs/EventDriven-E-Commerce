package com.eventdrivencommerce.product.application.model;

import java.util.Locale;

public record ProductSearchQuery(
        String name,
        String category,
        Boolean active,
        int page,
        int size,
        SortField sortField,
        SortDirection sortDirection
) {
    public ProductSearchQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        sortField = sortField == null ? SortField.NAME : sortField;
        sortDirection = sortDirection == null ? SortDirection.ASC : sortDirection;
        name = normalize(name);
        category = normalize(category);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public enum SortField {
        NAME("name"), CATEGORY("category"), PRICE("price"), CREATED_AT("createdAt"), UPDATED_AT("updatedAt");

        private final String property;

        SortField(String property) { this.property = property; }
        public String property() { return property; }

        public static SortField parse(String value) {
            try {
                return valueOf((value == null ? "NAME" : value).strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unsupported sort field: " + value);
            }
        }
    }

    public enum SortDirection {
        ASC, DESC;

        public static SortDirection parse(String value) {
            try {
                return valueOf((value == null ? "ASC" : value).strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Sort direction must be ASC or DESC");
            }
        }
    }
}

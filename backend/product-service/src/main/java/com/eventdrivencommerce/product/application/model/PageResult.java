package com.eventdrivencommerce.product.application.model;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public PageResult {
        items = List.copyOf(items);
    }
}

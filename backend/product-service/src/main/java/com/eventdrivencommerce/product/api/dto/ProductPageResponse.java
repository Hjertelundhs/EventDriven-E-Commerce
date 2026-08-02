package com.eventdrivencommerce.product.api.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public ProductPageResponse {
        items = List.copyOf(items);
    }
}

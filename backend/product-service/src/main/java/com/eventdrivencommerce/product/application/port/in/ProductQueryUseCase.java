package com.eventdrivencommerce.product.application.port.in;

import com.eventdrivencommerce.product.application.model.PageResult;
import com.eventdrivencommerce.product.application.model.ProductResult;
import com.eventdrivencommerce.product.application.model.ProductSearchQuery;

import java.util.UUID;

public interface ProductQueryUseCase {
    ProductResult get(UUID productId);
    PageResult<ProductResult> search(ProductSearchQuery query);
}

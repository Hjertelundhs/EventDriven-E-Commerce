package com.eventdrivencommerce.product.api;

import com.eventdrivencommerce.product.api.dto.ProductResponse;
import com.eventdrivencommerce.product.application.model.ProductResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductApiMapper {
    ProductResponse toResponse(ProductResult result);
}

package com.eventdrivencommerce.product.application.port.in;

import com.eventdrivencommerce.product.application.model.CreateProductCommand;
import com.eventdrivencommerce.product.application.model.DeactivateProductCommand;
import com.eventdrivencommerce.product.application.model.ProductResult;
import com.eventdrivencommerce.product.application.model.UpdateProductCommand;

public interface ProductCommandUseCase {
    ProductResult create(CreateProductCommand command);
    ProductResult update(UpdateProductCommand command);
    ProductResult deactivate(DeactivateProductCommand command);
}

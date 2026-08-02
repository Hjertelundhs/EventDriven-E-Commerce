package com.eventdrivencommerce.product.application.port.out;

import com.eventdrivencommerce.product.application.event.ProductChangedV1;

public interface ProductEventOutbox {
    void append(ProductChangedV1 event);
}

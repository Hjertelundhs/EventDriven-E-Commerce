package com.eventdrivencommerce.order.application.port.out;
import com.eventdrivencommerce.order.application.model.ProductSnapshot;
import java.util.UUID;
public interface ProductCatalog { ProductSnapshot get(UUID productId); }

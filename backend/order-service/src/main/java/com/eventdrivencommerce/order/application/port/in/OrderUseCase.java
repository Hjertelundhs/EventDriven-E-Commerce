package com.eventdrivencommerce.order.application.port.in;

import com.eventdrivencommerce.order.application.model.CreateOrderCommand;
import com.eventdrivencommerce.order.application.model.CreateOrderResult;
import com.eventdrivencommerce.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface OrderUseCase {
    CreateOrderResult create(CreateOrderCommand command);
    Order get(UUID orderId, UUID customerId);
    Page<Order> list(UUID customerId, Pageable pageable);
}

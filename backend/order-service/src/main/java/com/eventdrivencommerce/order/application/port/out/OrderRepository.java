package com.eventdrivencommerce.order.application.port.out;

import com.eventdrivencommerce.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    Optional<Order> findByCustomerAndIdempotencyKey(UUID customerId, String key);
    Page<Order> findByCustomer(UUID customerId, Pageable pageable);
}

package com.eventdrivencommerce.order.application.service;

import com.eventdrivencommerce.order.application.exception.*;
import com.eventdrivencommerce.order.application.model.*;
import com.eventdrivencommerce.order.application.port.in.OrderUseCase;
import com.eventdrivencommerce.order.application.port.out.*;
import com.eventdrivencommerce.order.domain.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.*;

@Service
public class OrderApplicationService implements OrderUseCase {
    private final OrderRepository repository; private final ProductCatalog products; private final OrderEventOutbox outbox; private final Clock clock;
    public OrderApplicationService(OrderRepository repository, ProductCatalog products, OrderEventOutbox outbox, Clock clock) {
        this.repository=repository; this.products=products; this.outbox=outbox; this.clock=clock;
    }
    @Override @Transactional
    public CreateOrderResult create(CreateOrderCommand command) {
        Optional<Order> existing = repository.findByCustomerAndIdempotencyKey(command.customerId(), command.idempotencyKey());
        if (existing.isPresent()) {
            if (!existing.get().requestFingerprint().equals(command.requestFingerprint())) throw new IdempotencyConflictException();
            return new CreateOrderResult(existing.get(), true);
        }
        List<OrderLine> lines = command.lines().stream().map(request -> {
            ProductSnapshot product = products.get(request.productId());
            if (!product.active()) throw new ProductValidationException("Product " + product.id() + " is inactive");
            if (!product.currency().equalsIgnoreCase(command.currency())) throw new ProductValidationException("All products must use order currency " + command.currency());
            return OrderLine.priced(product.id(), product.sku(), product.name(), request.quantity(), product.price());
        }).toList();
        var now = clock.instant();
        Order order = Order.create(command.customerId(), lines, command.currency(), command.shippingAddress(), command.billingAddress(),
                command.idempotencyKey(), command.requestFingerprint(), now);
        Order saved = repository.save(order);
        UUID correlation = command.correlationId() == null ? UUID.randomUUID() : command.correlationId();
        outbox.orderCreated(saved, correlation, correlation, now);
        return new CreateOrderResult(saved, false);
    }
    @Override @Transactional(readOnly=true)
    public Order get(UUID orderId, UUID customerId) { return repository.findById(orderId).filter(o -> o.customerId().equals(customerId)).orElseThrow(() -> new OrderNotFoundException(orderId)); }
    @Override @Transactional(readOnly=true)
    public Page<Order> list(UUID customerId, Pageable pageable) { return repository.findByCustomer(customerId, pageable); }
}

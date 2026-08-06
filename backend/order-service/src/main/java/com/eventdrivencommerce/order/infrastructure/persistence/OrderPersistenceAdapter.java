package com.eventdrivencommerce.order.infrastructure.persistence;
import com.eventdrivencommerce.order.application.port.out.OrderRepository;
import com.eventdrivencommerce.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
@Repository public class OrderPersistenceAdapter implements OrderRepository {
    private final SpringDataOrderRepository delegate;
    public OrderPersistenceAdapter(SpringDataOrderRepository delegate){this.delegate=delegate;}
    public Order save(Order order){var entity=delegate.findById(order.id()).orElseGet(()->OrderJpaEntity.from(order));entity.apply(order);return delegate.save(entity).toDomain();}
    public Optional<Order> findById(UUID id){return delegate.findById(id).map(OrderJpaEntity::toDomain);}
    public Optional<Order> findByCustomerAndIdempotencyKey(UUID customerId,String key){return delegate.findByCustomerIdAndIdempotencyKey(customerId,key).map(OrderJpaEntity::toDomain);}
    public Page<Order> findByCustomer(UUID customerId,Pageable pageable){return delegate.findByCustomerIdOrderByCreatedAtDesc(customerId,pageable).map(OrderJpaEntity::toDomain);}
}

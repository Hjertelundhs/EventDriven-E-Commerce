package com.eventdrivencommerce.order.infrastructure.persistence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity,UUID> {
    Optional<OrderJpaEntity> findByCustomerIdAndIdempotencyKey(UUID customerId,String key);
    Page<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
}

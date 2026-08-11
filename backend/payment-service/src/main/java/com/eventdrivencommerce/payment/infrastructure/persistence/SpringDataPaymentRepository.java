package com.eventdrivencommerce.payment.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional; import java.util.UUID;
interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity,UUID>{Optional<PaymentJpaEntity> findByOrderId(UUID orderId);}

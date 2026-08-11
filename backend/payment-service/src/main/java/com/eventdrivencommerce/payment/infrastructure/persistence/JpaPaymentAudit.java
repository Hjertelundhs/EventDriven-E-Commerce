package com.eventdrivencommerce.payment.infrastructure.persistence;
import com.eventdrivencommerce.payment.application.port.PaymentAudit;import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.stereotype.Component;import java.time.Instant;import java.util.UUID;
interface PaymentAuditRepository extends JpaRepository<PaymentAuditEntity,UUID>{}
@Component class JpaPaymentAudit implements PaymentAudit{private final PaymentAuditRepository repository;JpaPaymentAudit(PaymentAuditRepository repository){this.repository=repository;}public void record(UUID id,String action,String outcome,String detail,Instant at){repository.save(new PaymentAuditEntity(id,action,outcome,detail,at));}}

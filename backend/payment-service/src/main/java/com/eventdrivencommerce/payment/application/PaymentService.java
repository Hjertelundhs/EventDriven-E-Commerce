package com.eventdrivencommerce.payment.application;

import com.eventdrivencommerce.payment.application.port.*;
import com.eventdrivencommerce.payment.domain.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository payments; private final PaymentProvider provider; private final PaymentEventOutbox outbox; private final PaymentAudit audit; private final Clock clock;
    public PaymentService(PaymentRepository payments, PaymentProvider provider, PaymentEventOutbox outbox, PaymentAudit audit, Clock clock){this.payments=payments;this.provider=provider;this.outbox=outbox;this.audit=audit;this.clock=clock;}
    @Transactional public Payment registerOrder(UUID orderId, BigDecimal amount, String currency){
        return payments.findByOrderId(orderId).map(existing->{if(!existing.sameMoney(amount,currency))throw new IllegalStateException("Order payment projection conflicts with existing money");return existing;}).orElseGet(()->{var now=clock.instant();var saved=payments.save(Payment.pending(orderId,amount,currency,now));audit.record(saved.id(),"ORDER_REGISTERED","PENDING",null,now);return saved;});
    }
    @Transactional public Payment capture(UUID orderId, UUID correlation, UUID causation){
        Payment payment=payments.findByOrderId(orderId).orElseThrow(()->new PaymentNotReadyException(orderId)); if(payment.status()!=com.eventdrivencommerce.payment.domain.PaymentStatus.PENDING)return payment;
        var now=clock.instant();var result=provider.capture(orderId,payment.amount(),payment.currency(),"capture:"+orderId);
        if(result.successful()){payment.complete(result.providerReference(),now);payments.save(payment);audit.record(payment.id(),"CAPTURE","COMPLETED",null,now);outbox.paymentCompleted(payment,correlation,causation,now);}else{payment.fail(result.reasonCode(),now);payments.save(payment);audit.record(payment.id(),"CAPTURE","FAILED",result.reasonCode(),now);outbox.paymentFailed(payment,result.retryable(),correlation,causation,now);}return payment;
    }
    @Transactional public Payment refund(UUID orderId, String reason, UUID correlation, UUID causation){
        Payment payment=payments.findByOrderId(orderId).orElseThrow(()->new PaymentNotReadyException(orderId)); if(payment.status()==com.eventdrivencommerce.payment.domain.PaymentStatus.REFUNDED||payment.status()==com.eventdrivencommerce.payment.domain.PaymentStatus.REFUND_FAILED)return payment;
        var now=clock.instant();UUID refundId=payment.requestRefund(now);payments.save(payment);audit.record(payment.id(),"REFUND_REQUESTED","PENDING",reason,now);outbox.refundRequested(payment,reason,correlation,causation,now);
        var result=provider.refund(payment.id(),refundId,payment.amount(),payment.currency(),"refund:"+refundId);now=clock.instant();
        if(result.successful()){payment.refundComplete(result.providerReference(),now);payments.save(payment);audit.record(payment.id(),"REFUND","COMPLETED",null,now);outbox.refundCompleted(payment,correlation,causation,now);}else{payment.refundFail(result.reasonCode(),now);payments.save(payment);audit.record(payment.id(),"REFUND","FAILED",result.reasonCode(),now);outbox.refundFailed(payment,result.retryable(),correlation,causation,now);}return payment;
    }
    @Transactional(readOnly=true) public Payment get(UUID id){return payments.findById(id).orElseThrow(()->new PaymentNotReadyException(id));}
    @Transactional(readOnly=true) public Payment getByOrder(UUID orderId){return payments.findByOrderId(orderId).orElseThrow(()->new PaymentNotReadyException(orderId));}
}

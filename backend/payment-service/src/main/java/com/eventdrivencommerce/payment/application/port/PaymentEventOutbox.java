package com.eventdrivencommerce.payment.application.port;
import com.eventdrivencommerce.payment.domain.Payment; import java.time.Instant; import java.util.UUID;
public interface PaymentEventOutbox {
    void paymentCompleted(Payment payment, UUID correlation, UUID causation, Instant now);
    void paymentFailed(Payment payment, boolean retryable, UUID correlation, UUID causation, Instant now);
    void refundRequested(Payment payment, String reason, UUID correlation, UUID causation, Instant now);
    void refundCompleted(Payment payment, UUID correlation, UUID causation, Instant now);
    void refundFailed(Payment payment, boolean retryable, UUID correlation, UUID causation, Instant now);
}

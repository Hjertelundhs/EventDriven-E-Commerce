package com.eventdrivencommerce.payment.application.port;
import java.math.BigDecimal; import java.util.UUID;
public interface PaymentProvider {
    ProviderResult capture(UUID orderId, BigDecimal amount, String currency, String idempotencyKey);
    ProviderResult refund(UUID paymentId, UUID refundId, BigDecimal amount, String currency, String idempotencyKey);
    record ProviderResult(boolean successful, String providerReference, String reasonCode, boolean retryable) {
        public static ProviderResult success(String reference){return new ProviderResult(true,reference,null,false);}
        public static ProviderResult failure(String reason,boolean retryable){return new ProviderResult(false,null,reason,retryable);}
    }
}

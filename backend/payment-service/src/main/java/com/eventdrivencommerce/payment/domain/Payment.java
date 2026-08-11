package com.eventdrivencommerce.payment.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class Payment {
    private final UUID id;
    private final UUID orderId;
    private final BigDecimal amount;
    private final String currency;
    private PaymentStatus status;
    private String providerReference;
    private String failureReason;
    private UUID refundId;
    private String refundProviderReference;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(UUID id, UUID orderId, BigDecimal amount, String currency, PaymentStatus status,
                    String providerReference, String failureReason, UUID refundId,
                    String refundProviderReference, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id); this.orderId = Objects.requireNonNull(orderId);
        this.amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        this.currency = currency.toUpperCase(Locale.ROOT); this.status = status;
        this.providerReference = providerReference; this.failureReason = failureReason;
        this.refundId = refundId; this.refundProviderReference = refundProviderReference;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
        if (this.amount.signum() < 0 || !this.currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("Invalid payment money");
    }

    public static Payment pending(UUID orderId, BigDecimal amount, String currency, Instant now) {
        return new Payment(UUID.randomUUID(), orderId, amount, currency, PaymentStatus.PENDING, null, null, null, null, now, now);
    }
    public static Payment restore(UUID id, UUID orderId, BigDecimal amount, String currency, PaymentStatus status,
                                  String providerReference, String failureReason, UUID refundId,
                                  String refundProviderReference, Instant createdAt, Instant updatedAt) {
        return new Payment(id, orderId, amount, currency, status, providerReference, failureReason, refundId, refundProviderReference, createdAt, updatedAt);
    }
    public void complete(String reference, Instant now) { require(PaymentStatus.PENDING); providerReference = requireReference(reference); status = PaymentStatus.COMPLETED; updatedAt = now; }
    public void fail(String reason, Instant now) { require(PaymentStatus.PENDING); failureReason = safeReason(reason); status = PaymentStatus.FAILED; updatedAt = now; }
    public UUID requestRefund(Instant now) { require(PaymentStatus.COMPLETED); refundId = UUID.nameUUIDFromBytes(("refund:" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8)); status = PaymentStatus.REFUND_PENDING; updatedAt = now; return refundId; }
    public void refundComplete(String reference, Instant now) { require(PaymentStatus.REFUND_PENDING); refundProviderReference = requireReference(reference); status = PaymentStatus.REFUNDED; updatedAt = now; }
    public void refundFail(String reason, Instant now) { require(PaymentStatus.REFUND_PENDING); failureReason = safeReason(reason); status = PaymentStatus.REFUND_FAILED; updatedAt = now; }
    public boolean sameMoney(BigDecimal otherAmount, String otherCurrency) { return amount.compareTo(otherAmount) == 0 && currency.equalsIgnoreCase(otherCurrency); }
    private void require(PaymentStatus expected) { if (status != expected) throw new IllegalStateException("Payment state " + status + " does not allow this transition"); }
    private static String requireReference(String value) { if (value == null || !value.matches("[A-Za-z0-9_-]{8,120}")) throw new IllegalArgumentException("Invalid provider reference"); return value; }
    private static String safeReason(String value) { return value == null || !value.matches("[A-Z0-9_]{3,64}") ? "PROVIDER_ERROR" : value; }
    public UUID id(){return id;} public UUID orderId(){return orderId;} public BigDecimal amount(){return amount;} public String currency(){return currency;} public PaymentStatus status(){return status;} public String providerReference(){return providerReference;} public String failureReason(){return failureReason;} public UUID refundId(){return refundId;} public String refundProviderReference(){return refundProviderReference;} public Instant createdAt(){return createdAt;} public Instant updatedAt(){return updatedAt;}
}

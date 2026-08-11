package com.eventdrivencommerce.payment.application.port;
import java.time.Instant; import java.util.UUID;
public interface PaymentAudit { void record(UUID paymentId, String action, String outcome, String safeDetail, Instant at); }

package com.eventdrivencommerce.payment.application;
import java.util.UUID;
public class PaymentNotReadyException extends RuntimeException { public PaymentNotReadyException(UUID orderId){super("Payment projection is not ready for order " + orderId);} }

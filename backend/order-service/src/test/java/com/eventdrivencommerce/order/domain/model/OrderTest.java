package com.eventdrivencommerce.order.domain.model;
import com.eventdrivencommerce.order.domain.exception.InvalidOrderTransitionException;import org.junit.jupiter.api.Test;import java.math.BigDecimal;import java.time.Instant;import java.util.*;import static org.assertj.core.api.Assertions.*;
class OrderTest {
 private static final Instant NOW=Instant.parse("2026-01-01T00:00:00Z");
 @Test void completesHappyPath(){Order o=order();o.inventoryReserved(NOW.plusSeconds(1));assertThat(o.status()).isEqualTo(OrderStatus.PAYMENT_PENDING);o.paymentCompleted(NOW.plusSeconds(2));o.deliveryCreated(NOW.plusSeconds(3));o.delivered(NOW.plusSeconds(4));assertThat(o.status()).isEqualTo(OrderStatus.COMPLETED);}
 @Test void cancelsAfterPaymentFailureAndInventoryRelease(){Order o=order();o.inventoryReserved(NOW);o.paymentFailed(NOW);o.inventoryReleased(NOW);assertThat(o.status()).isEqualTo(OrderStatus.CANCELLED);assertThat(o.cancellationReason()).isEqualTo("DOWNSTREAM_FAILURE");}
 @Test void deliveryCompensationNeedsRefundAndRelease(){Order o=order();o.inventoryReserved(NOW);o.paymentCompleted(NOW);o.deliveryFailed(NOW);o.inventoryReleased(NOW);assertThat(o.status()).isEqualTo(OrderStatus.DELIVERY_FAILED);o.refundCompleted(NOW);assertThat(o.status()).isEqualTo(OrderStatus.CANCELLED);}
 @Test void rejectsOutOfOrderEvents(){assertThatThrownBy(()->order().paymentCompleted(NOW)).isInstanceOf(InvalidOrderTransitionException.class);}
 private static Order order(){var line=OrderLine.priced(UUID.randomUUID(),"SKU-1","Item",2,new BigDecimal("10.00"));var a=new Address("Customer","Street 1","","12345","Stockholm","SE");return Order.create(UUID.randomUUID(),List.of(line),"SEK",a,a,"key","fingerprint",NOW);}
}

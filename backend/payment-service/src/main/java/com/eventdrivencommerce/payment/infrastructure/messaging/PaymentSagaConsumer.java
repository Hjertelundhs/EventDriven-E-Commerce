package com.eventdrivencommerce.payment.infrastructure.messaging;
import com.eventdrivencommerce.payment.application.PaymentService;import com.fasterxml.jackson.databind.*;import org.springframework.kafka.annotation.KafkaListener;import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;import java.math.BigDecimal;import java.time.Clock;import java.util.UUID;
@Component public class PaymentSagaConsumer{
 static final String GROUP="payment-service-v1";private final ObjectMapper mapper;private final PaymentService payments;private final ProcessedEventRepository processed;private final Clock clock;
 public PaymentSagaConsumer(ObjectMapper mapper,PaymentService payments,ProcessedEventRepository processed,Clock clock){this.mapper=mapper;this.payments=payments;this.processed=processed;this.clock=clock;}
 @KafkaListener(topics={"commerce.order.v1","commerce.inventory.v1","commerce.delivery.v1"},groupId=GROUP)@Transactional public void consume(String json)throws Exception{
  JsonNode event=mapper.readTree(json);String type=event.path("eventType").asText();if(!type.equals("OrderCreatedV1")&&!type.equals("InventoryReservedV1")&&!type.equals("DeliveryFailedV1"))return;UUID eventId=uuid(event,"eventId");var key=new ProcessedEventId(GROUP,eventId);if(processed.existsById(key))return;JsonNode payload=event.path("payload");UUID orderId=UUID.fromString(payload.path("orderId").asText());UUID correlation=event.hasNonNull("correlationId")?uuid(event,"correlationId"):UUID.randomUUID();
  switch(type){case "OrderCreatedV1"->payments.registerOrder(orderId,new BigDecimal(payload.path("totalAmount").asText()),payload.path("currency").asText());case "InventoryReservedV1"->payments.capture(orderId,correlation,eventId);case "DeliveryFailedV1"->payments.refund(orderId,payload.path("reasonCode").asText("DELIVERY_FAILED"),correlation,eventId);default->throw new IllegalStateException("Unsupported payment event");}
  processed.save(new ProcessedEventJpaEntity(key,type,clock.instant()));
 }
 private static UUID uuid(JsonNode n,String field){return UUID.fromString(n.path(field).asText());}
}

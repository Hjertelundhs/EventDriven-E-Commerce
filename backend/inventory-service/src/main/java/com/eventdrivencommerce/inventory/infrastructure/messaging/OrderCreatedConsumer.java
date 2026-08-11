package com.eventdrivencommerce.inventory.infrastructure.messaging;

import com.eventdrivencommerce.inventory.application.model.OrderInventoryRequest;
import com.eventdrivencommerce.inventory.application.service.OrderInventorySagaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.ArrayList;
import java.util.UUID;

@Component
public class OrderCreatedConsumer {
    static final String GROUP="inventory-order-saga-v1";
    private final ObjectMapper mapper; private final OrderInventorySagaService saga; private final ProcessedEventRepository processed; private final Clock clock;
    public OrderCreatedConsumer(ObjectMapper mapper,OrderInventorySagaService saga,ProcessedEventRepository processed,Clock clock){this.mapper=mapper;this.saga=saga;this.processed=processed;this.clock=clock;}
    @KafkaListener(topics="commerce.order.v1",groupId=GROUP)
    @Transactional
    public void consume(String json) throws Exception {
        JsonNode event=mapper.readTree(json); String type=event.path("eventType").asText();
        if(!"OrderCreatedV1".equals(type)) return;
        UUID eventId=uuid(event,"eventId"); var id=new ProcessedEventId(GROUP,eventId); if(processed.existsById(id)) return;
        JsonNode payload=event.path("payload"); var lines=new ArrayList<OrderInventoryRequest.Line>();
        for(JsonNode line:payload.path("lines")) lines.add(new OrderInventoryRequest.Line(line.path("sku").asText(),line.path("quantity").asInt()));
        UUID correlation=event.hasNonNull("correlationId")?uuid(event,"correlationId"):UUID.randomUUID();
        saga.reserve(new OrderInventoryRequest(eventId,uuid(payload,"orderId"),correlation,lines));
        processed.save(new ProcessedEventJpaEntity(id,type,clock.instant()));
    }
    private static UUID uuid(JsonNode node,String field){return UUID.fromString(node.path(field).asText());}
}

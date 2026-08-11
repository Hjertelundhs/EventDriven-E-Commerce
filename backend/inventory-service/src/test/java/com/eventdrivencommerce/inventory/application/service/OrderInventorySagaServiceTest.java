package com.eventdrivencommerce.inventory.application.service;

import com.eventdrivencommerce.inventory.application.model.OrderInventoryRequest;
import com.eventdrivencommerce.inventory.application.port.out.*;
import com.eventdrivencommerce.inventory.domain.model.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderInventorySagaServiceTest {
    private final InventoryRepository inventory=mock(InventoryRepository.class); private final ReservationRepository reservations=mock(ReservationRepository.class); private final InventoryEventOutbox outbox=mock(InventoryEventOutbox.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"),ZoneOffset.UTC); private final OrderInventorySagaService service=new OrderInventorySagaService(inventory,reservations,outbox,clock);
    @Test void reservesAllLinesAndPublishesSuccess(){UUID order=UUID.randomUUID(),event=UUID.randomUUID(),correlation=UUID.randomUUID();InventoryItem item=InventoryItem.create(UUID.randomUUID(),new Sku("SKU-1"),clock.instant());item.receive(10,clock.instant());when(inventory.findBySku(new Sku("SKU-1"))).thenReturn(Optional.of(item));when(reservations.findByOrderIdAndSku(order,new Sku("SKU-1"))).thenReturn(Optional.empty());when(inventory.save(item)).thenReturn(item);when(reservations.save(any())).thenAnswer(i->i.getArgument(0));service.reserve(new OrderInventoryRequest(event,order,correlation,List.of(new OrderInventoryRequest.Line("SKU-1",2))));assertThat(item.availableQuantity()).isEqualTo(8);verify(reservations).save(any());verify(outbox).reserved(eq(order),anyList(),eq(correlation),eq(event),eq(clock.instant()));}
    @Test void publishesFailureWithoutChangingAnyStock(){UUID order=UUID.randomUUID(),event=UUID.randomUUID(),correlation=UUID.randomUUID();InventoryItem item=InventoryItem.create(UUID.randomUUID(),new Sku("SKU-1"),clock.instant());item.receive(1,clock.instant());when(inventory.findBySku(new Sku("SKU-1"))).thenReturn(Optional.of(item));service.reserve(new OrderInventoryRequest(event,order,correlation,List.of(new OrderInventoryRequest.Line("SKU-1",2))));assertThat(item.availableQuantity()).isEqualTo(1);verifyNoInteractions(reservations);verify(inventory,never()).save(any());verify(outbox).reservationFailed(eq(order),anyList(),eq(correlation),eq(event),eq(clock.instant()));}
}

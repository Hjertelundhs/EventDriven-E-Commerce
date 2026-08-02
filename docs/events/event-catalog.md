# Eventkatalog

## Kontraktsstrategi

Events serialiseras som JSON och valideras mot JSON Schema Draft 2020-12. Scheman publiceras i `backend/shared-contracts`; `ProductChangedV1` levereras i Fas 3 som `events/product-changed-v1.schema.json`. Kompatibilitetsregeln är backward compatible inom samma majorversion: nya optional fields får läggas till, befintliga fält får inte byta betydelse eller typ och enum-utökningar behandlas som potentiellt brytande för konsumenter.

Brytande förändringar får ett nytt eventnamn, exempelvis `OrderCreatedV2`, och samexisterar under en migrationsperiod. Konsumenter ignorerar okända optional fields men avvisar okända majorversioner till DLT.

## Standard-envelope

Alla events innehåller exakt följande gemensamma metadata samt en eventspecifik `payload`:

```json
{
  "eventId": "018f5f5d-7d98-7bd7-9f0f-f6cb53b07913",
  "eventType": "OrderCreatedV1",
  "eventVersion": 1,
  "aggregateId": "018f5f5b-d133-76b8-a57c-f1618d74c45e",
  "correlationId": "018f5f58-e584-7b04-b9ac-2e5c7d6e9f70",
  "causationId": "018f5f59-441d-7495-afd1-e72485bd4f67",
  "occurredAt": "2026-08-02T12:00:00.000Z",
  "payload": {}
}
```

| Fält | Regel |
|---|---|
| `eventId` | Globalt unik UUID, stabil över producer retry |
| `eventType` | Versionssuffix ingår i namnet |
| `eventVersion` | Positiv integer och överensstämmer med suffixet |
| `aggregateId` | Producerande aggregate-ID; för sagaevents bär payload alltid `orderId` |
| `correlationId` | Stabilt saga-/request-ID genom hela orderflödet |
| `causationId` | Event-ID eller command/request-ID som direkt orsakade eventet |
| `occurredAt` | UTC, RFC 3339 med millisekunder |
| `payload` | Händelsespecifik, immutable fakta utan credentials eller betalningshemligheter |

Kafkaheaders bär `traceparent`, optional `tracestate`, content type, schema identifier och producer service. Headers ersätter inte envelopefält som behövs vid replay.

## Topics

| Topic | Ägare | Nyckel | Producent | Retention lokalt | DLT |
|---|---|---|---|---|---|
| `commerce.order.v1` | Order | `orderId` | Order Service | 7 dagar | `commerce.order.v1.dlt` |
| `commerce.inventory.v1` | Inventory | `orderId` för saga, annars `sku` | Inventory Service | 7 dagar | `commerce.inventory.v1.dlt` |
| `commerce.payment.v1` | Payment | `orderId` | Payment Service | 30 dagar | `commerce.payment.v1.dlt` |
| `commerce.delivery.v1` | Delivery | `orderId` | Delivery Service | 30 dagar | `commerce.delivery.v1.dlt` |
| `commerce.product.v1` | Product | `productId` | Product Service | kompakterad + 7 dagar | `commerce.product.v1.dlt` |

Alla sagaevents partitioneras på `orderId`. Det ger ordning per order inom en topic, men inte global ordning över topics; konsumenter måste därför validera state och tåla omleverans. Antal partitioner fastställs i Fas 2 utifrån lokal drift och kan höjas utan att kontrakten ändras.

DLT-meddelanden behåller originalpayload och headers samt lägger till felklass, konsument, attempts och första/sista feltid. Stack traces och hemligheter ska inte läggas i headers. DLT återspelas via ett explicit, auditerat verktyg till originaltopic efter att orsaken åtgärdats.

## Order events

### `OrderCreatedV1`

**Topic:** `commerce.order.v1`<br>
**Producent:** Order Service<br>
**Konsumenter:** Inventory Service, Payment Service, Delivery Service, Notification Service<br>
**Payload:** `orderId`, `customerId`, `lines[] { productId, sku, productName, quantity, unitPrice, lineTotal }`, `totalAmount`, `currency`, `shippingAddress`.

Shippingadressen är den enda adress som krävs för fulfillment och får därför följa eventet; billingadressen stannar i Order Service. Loggning och telemetry måste redigera payloaden. Payment och Delivery lagrar endast sina nödvändiga fält som väntande lokal projektion. De utför ingen betalning eller leverans förrän respektive triggande event anländer.

### `OrderCompletedV1`

**Konsumenter:** Inventory Service, Notification Service.<br>
**Payload:** `orderId`, `customerId`, `completedAt`.

Inventory Service slutför reservationen. Händelsen är orderns terminala framgångsfakta.

### `OrderCancelledV1`

**Konsumenter:** Notification Service och auditprojektion.<br>
**Payload:** `orderId`, `customerId`, `reasonCode`, `cancelledAt`.

Kompensationer triggas av deras ursprungliga felhändelse, inte av detta generella event, vilket undviker tvetydig dubbelkompensation.

## Inventory events

### `InventoryReservedV1`

**Konsumenter:** Order Service, Payment Service.<br>
**Payload:** `reservationId`, `orderId`, `items[] { sku, quantity }`, `reservedAt`, `expiresAt`.

### `InventoryReservationFailedV1`

**Konsumenter:** Order Service, Notification Service.<br>
**Payload:** `orderId`, `failures[] { sku, requestedQuantity, availableQuantity, reasonCode }`, `failedAt`.

### `InventoryReleasedV1`

**Konsumenter:** Order Service.<br>
**Payload:** `reservationId`, `orderId`, `items[] { sku, quantity }`, `releaseReason`, `releasedAt`.

### `InventoryReservationCompletedV1`

**Konsumenter:** auditprojektion.<br>
**Payload:** `reservationId`, `orderId`, `items[] { sku, quantity }`, `completedAt`.

## Payment events

### `PaymentCompletedV1`

**Konsumenter:** Order Service, Delivery Service, Notification Service.<br>
**Payload:** `paymentId`, `orderId`, `amount`, `currency`, `providerReference`, `completedAt`.

`providerReference` är en simulerad opak referens, aldrig kortdata.

### `PaymentFailedV1`

**Konsumenter:** Order Service, Inventory Service, Notification Service.<br>
**Payload:** `paymentId`, `orderId`, `amount`, `currency`, `reasonCode`, `retryable`, `failedAt`.

`retryable` beskriver providerutfallet; teknisk konsumentretry styrs separat.

### `RefundRequestedV1`

**Konsumenter:** Payment Services refund-adapter, Notification Service/audit.<br>
**Payload:** `refundId`, `paymentId`, `orderId`, `amount`, `currency`, `reasonCode`, `requestedAt`.

### `RefundCompletedV1`

**Konsumenter:** Order Service, Notification Service/audit.<br>
**Payload:** `refundId`, `paymentId`, `orderId`, `amount`, `currency`, `providerReference`, `completedAt`.

### `RefundFailedV1`

**Konsumenter:** Order Service, operatörslarm.<br>
**Payload:** `refundId`, `paymentId`, `orderId`, `reasonCode`, `retryable`, `failedAt`.

## Delivery events

### `DeliveryCreatedV1`

**Konsumenter:** Order Service, Notification Service.<br>
**Payload:** `deliveryId`, `orderId`, `trackingNumber`, `estimatedDeliveryDate`, `createdAt`.

### `DeliveryFailedV1`

**Konsumenter:** Order Service, Payment Service, Inventory Service, Notification Service.<br>
**Payload:** `deliveryId` (optional om skapande aldrig hann ske), `orderId`, `reasonCode`, `failedAt`.

### `DeliveryStatusChangedV1`

**Konsumenter:** Order Service, Notification Service.<br>
**Payload:** `deliveryId`, `orderId`, `trackingNumber`, `previousStatus`, `status`, `changedAt`.

Tillåtna statusvärden är `CREATED`, `READY_FOR_PICKUP`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`, `DELIVERED`, `FAILED` och `RETURNED`.

## Product events

### `ProductChangedV1`

**Konsumenter:** Product Services cacheinvaliderare och framtida read models.<br>
**Payload:** `productId`, `sku`, `changeType` (`CREATED`, `UPDATED`, `DEACTIVATED`), `productVersion`, `changedAt`.

Eventet innehåller inte hela beskrivningen eller priset. Cacheinvalidering tar bort berörda nycklar; nästa läsning fyller cachen från Product Service-databasen.

## Consumer groups

Varje logisk projektion eller affärseffekt har en egen stabil consumer group, exempelvis `inventory-order-saga-v1`, `payment-order-saga-v1`, `delivery-order-saga-v1`, `order-saga-projection-v1` och `notification-domain-events-v1`. Flera instanser av samma tjänst delar group; olika affärseffekter delar aldrig group enbart för att spara resurser.

## Dataminimering och retention

Events är immutable och svåra att radera selektivt. Payloads använder interna identiteter och minsta nödvändiga persondata. Notifiering hämtar aktuell kontaktkanal från Identity Service när utskick behandlas i stället för att sprida e-postadress till alla topics. Produktionsretention, legal basis och data subject-process dokumenteras i säkerhetsfasen innan riktig persondata används.

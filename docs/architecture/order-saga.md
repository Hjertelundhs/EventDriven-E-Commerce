# Orderflöde och choreography-saga

## Affärsgräns

Orderregistreringen är den enda synkrona delen av kärnflödet. När aktuell produkt- och prisinformation har verifierats sparar Order Service ett `Order`-aggregate i `PENDING` och en `OrderCreatedV1`-outboxpost i samma transaktion. Klienten får `202 Accepted`, order-ID och en statuslänk. Därefter drivs flödet av fakta i Kafka.

## Lyckat flöde

1. Order Service publicerar `OrderCreatedV1`.
2. Inventory Service reserverar alla orderrader atomiskt per order. Payment och Delivery lagrar samtidigt minsta nödvändiga lokala väntande data men utför ingen extern effekt. Dubbel konsumtion träffar samma affärsnyckel.
3. Inventory Service publicerar `InventoryReservedV1`.
4. Order Service går `PENDING → INVENTORY_RESERVED → PAYMENT_PENDING`.
5. Payment Service skapar eller återanvänder betalning med saga-ID som idempotency key, auktoriserar och capturar.
6. Payment Service publicerar `PaymentCompletedV1`.
7. Order Service går `PAYMENT_PENDING → PAID`.
8. Delivery Service skapar exakt en leverans och publicerar `DeliveryCreatedV1`.
9. Order Service går `PAID → DELIVERY_CREATED`.
10. Delivery Service publicerar `DeliveryStatusChangedV1` för tillåtna statusövergångar.
11. När leveransen är `DELIVERED` går ordern `DELIVERY_CREATED → COMPLETED` och Order Service publicerar `OrderCompletedV1`.
12. Inventory Service slutför reservationen idempotent när ordern är fullbordad.

Notification Service observerar relevanta fakta och skickar e-post oberoende av sagans utfall.

## Kompensation: lager saknas

1. Inventory Service kan inte reservera hela ordern och gör ingen delreservation bestående.
2. Den publicerar `InventoryReservationFailedV1` med säkra reason codes.
3. Order Service går `PENDING → CANCELLED` och publicerar `OrderCancelledV1`.
4. Payment- och Delivery Service gör ingenting eftersom deras förutsättningshändelser aldrig publicerades.

## Kompensation: betalning misslyckas

1. Payment Service publicerar `PaymentFailedV1`; ett deterministiskt avslag är ett affärsutfall, inte ett retrybart tekniskt fel.
2. Order Service går `PAYMENT_PENDING → PAYMENT_FAILED`.
3. Inventory Service konsumerar händelsen, släpper reservationen exakt en gång och publicerar `InventoryReleasedV1`.
4. Order Service går `PAYMENT_FAILED → CANCELLED` när frisläppningen observerats och publicerar `OrderCancelledV1`.

## Kompensation: leverans kan inte skapas

1. Delivery Service publicerar `DeliveryFailedV1` efter att tekniska retries är uttömda eller ett permanent affärsfel konstaterats.
2. Order Service går `PAID → DELIVERY_FAILED`.
3. Payment Service markerar betalningen `REFUND_PENDING` och publicerar `RefundRequestedV1` atomiskt.
4. Refund-adaptern genomför en idempotent återbetalning och publicerar `RefundCompletedV1` eller `RefundFailedV1`.
5. Inventory Service släpper eller återför reservationen idempotent och publicerar `InventoryReleasedV1`.
6. När Order Service observerat både lyckad refund och frisläppt lager går ordern `DELIVERY_FAILED → CANCELLED` och publicerar `OrderCancelledV1`. Vid `RefundFailedV1` stannar ordern i `DELIVERY_FAILED`, larmas och kräver kontrollerad operatörsåtgärd.

## State machine

| Från | Trigger | Till |
|---|---|---|
| — | order accepterad | `PENDING` |
| `PENDING` | `InventoryReservedV1` | `INVENTORY_RESERVED` |
| `INVENTORY_RESERVED` | betalningsarbete initierat | `PAYMENT_PENDING` |
| `PAYMENT_PENDING` | `PaymentCompletedV1` | `PAID` |
| `PAID` | `DeliveryCreatedV1` | `DELIVERY_CREATED` |
| `DELIVERY_CREATED` | leverans `DELIVERED` | `COMPLETED` |
| `PENDING` | `InventoryReservationFailedV1` | `CANCELLED` |
| `PAYMENT_PENDING` | `PaymentFailedV1` | `PAYMENT_FAILED` |
| `PAYMENT_FAILED` | `InventoryReleasedV1` | `CANCELLED` |
| `PAID` | `DeliveryFailedV1` | `DELIVERY_FAILED` |
| `DELIVERY_FAILED` | refund + lagerkompensation klara | `CANCELLED` |

Övriga övergångar nekas. Duplicerade triggers returnerar det redan uppnådda resultatet utan ny domänhändelse. Händelser som anländer före sin kausala föregångare lagras inte blint som godkänd state; de retryas kort eller hamnar i en avvikelsekö för utredning beroende på typ.

## Idempotens och concurrency

- `eventId` har unik constraint i varje konsuments `processed_events`.
- Domänändring och processed-event-post sparas i samma lokala transaktion.
- Reservation är unik på `orderId`; payment och delivery har motsvarande affärsnycklar.
- Om en triggande händelse når Payment eller Delivery före dess lokala orderprojektion används en begränsad retry/parking-mekanism; ingen tom eller ofullständig affärseffekt skapas.
- Extern payment/refund använder stabil idempotency key, aldrig ett nytt värde per retry.
- Aggregateversion och optimistic locking hindrar parallella statusövergångar från att skriva över varandra.
- En kompensation kontrollerar aktuell state och är no-op om önskat slutläge redan är uppnått.

## Choreography kontra orkestrering

Vald choreography gör att varje tjänst reagerar på affärsfakta och minskar beroendet av en central processmotor. Nackdelen är att helhetsflödet är svårare att se och ändringar måste kompatibilitetstestas över flera konsumenter.

En orkestrerad variant skulle införa en `OrderSagaOrchestrator` som lagrar saga-state och skickar explicita kommandon som `ReserveInventory`, `CapturePayment`, `CreateDelivery`, `ReleaseInventory` och `RefundPayment`. Tjänsterna svarar med resultat-events, och orchestratorn väljer nästa steg eller kompensation. Det ger central timeout-/processöverblick men skapar en kritisk komponent och starkare processkoppling. Domänhändelser kan fortfarande publiceras för externa observatörer. Ett sådant byte kräver en ny ADR och görs inte i Fas 1.

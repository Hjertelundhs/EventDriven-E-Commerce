# ADR-003: Choreography-baserad order-saga

- **Status:** Accepted
- **Datum:** 2026-08-02

## Kontext

En order kräver lokala transaktioner i Order, Inventory, Payment och Delivery Service. En distribuerad databastransaktion är olämplig över självständiga tjänster. Misslyckanden kräver affärskompensation: avbryt order, släpp lager och återbetala betalning.

## Beslut

Orderflödet implementeras som choreography via Kafka. Varje tjänst reagerar på publicerade affärsfakta, utför en lokal transaktion och publicerar nästa fakta genom sin outbox. Ingen central komponent skickar alla stegkommandon.

Kompensationer är explicita och idempotenta:

- `InventoryReservationFailedV1` avbryter ordern.
- `PaymentFailedV1` får Inventory Service att släppa reservationen.
- `DeliveryFailedV1` får Payment Service att initiera refund och Inventory Service att återföra reservationen.
- Order Service sammanställer observerad saga-progress för kundstatus och terminal övergång, men orkestrerar inte genom commands.

## Konsekvenser

### Positiva

- Ingen central processmotor blir en runtimeflaskhals eller single point of failure.
- Tjänsterna behåller sina egna regler och reagerar på stabila fakta.
- Nya observatörer som Notification kan läggas till utan producentändring.

### Negativa och risker

- Processen är distribuerad och kräver god eventkatalog, tracing och dashboards.
- Cykler och oavsiktliga reaktioner kan uppstå om eventsemantik är vag.
- Timeouts och väntande kompensationer är svårare att hantera än i en central workflowmotor.
- Ändringar kräver kontraktstest mellan berörda tjänster.

## Övervägda alternativ

- **Orkestrerad saga:** central orchestrator skickar commands och lagrar state. Den ger tydlig processöverblick och timeouthantering men ökar central koppling och är inte förstahandskravet. En möjlig modell dokumenteras i order-sagadokumentet.
- **2PC/XA:** avvisas på grund av koppling, tillgänglighetskostnad och bristande stöd över externa providers.
- **Manuell kompensation enbart:** otillräckligt för normal felhantering men behålls som auditerad nödrutin för permanenta refundfel.

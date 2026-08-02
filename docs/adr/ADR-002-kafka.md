# ADR-002: Kafka för asynkrona domänhändelser

- **Status:** Accepted
- **Datum:** 2026-08-02

## Kontext

Orderflödet är långlivat, har flera oberoende konsumenter och måste tåla tillfällig otillgänglighet, omleverans och replay. Notifiering och audit ska kunna observera samma fakta utan att producenten känner till dem. Lösningen behöver partitionerad ordning och tydlig consumer-group-semantik.

## Beslut

Apache Kafka används som händelsetransport. Producenten äger sin domäntopic, eventtyper versioneras i namnet och sagaevents partitioneras på `orderId`. Leverans är at-least-once; exakt en affärseffekt skapas med idempotenta konsumenter och lokala unika constraints.

JSON med JSON Schema Draft 2020-12 väljs initialt för läsbarhet, portfolioinspektion och enkel frontend-/verktygsintegration. Schema identifieras i Kafkaheaders och stabila scheman lagras i `shared-contracts`. Brytande kontrakt får ny major eventversion.

Varje logisk affärseffekt använder en separat consumer group. Tekniska retries är begränsade och permanenta fel går till en topic-specifik DLT med auditmetadata.

## Konsekvenser

### Positiva

- Producenter och konsumenter kan vara temporärt frikopplade.
- Händelser kan konsumeras av flera projektioner och spelas om inom retention.
- Partitionering ger skalning och ordning per nyckel inom en topic.
- Kafka stöder tracingheaders, lagmetrics och etablerade driftverktyg.

### Negativa och risker

- Ingen global ordning finns mellan topics.
- Schemaevolution, lag, DLT och replay kräver operativ disciplin.
- JSON ger större payload och svagare binär typning än Avro/Protobuf.
- Kafka är inte ett affärssystem of record; retention får inte ersätta tjänstedatabaser.

## Övervägda alternativ

- **RabbitMQ:** stark kö-/routingmodell och enklare command queues, men sämre matchning för replaybara domänströmmar och konsumentgrupper i detta projekt.
- **REST-only:** enklare mental modell men skapar synkron kedjekoppling och gör långlivad saga skör.
- **Avro + Schema Registry:** kompakt och moget för strikt governance; kan införas senare om throughput eller organisationskrav motiverar den extra infrastrukturen.

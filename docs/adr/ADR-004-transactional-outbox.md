# ADR-004: Transactional outbox för eventpublicering

- **Status:** Accepted
- **Datum:** 2026-08-02

## Kontext

En tjänst måste både spara en domänändring i PostgreSQL och publicera en Kafka-händelse. Att göra dessa operationer separat skapar dual-write-fel: databasen kan committa utan event eller eventet kan publiceras utan bestående state. XA mellan PostgreSQL och Kafka väljs bort.

## Beslut

Alla eventproducerande tjänster använder transactional outbox. Aggregateändring och en komplett outboxrad sparas i samma lokala databastransaktion. En schemalagd publisher hämtar opublicerade rader i små batcher med konkurrenssäker låsning, publicerar med stabilt `eventId` och markerar publiceringstid efter broker-ack.

Outboxraden innehåller event-ID, aggregate type/id, event type/version, topic, partition key, payload, headers, created/published timestamps, attempt count och senaste sanerade felkod. Publishern återupptar opublicerade rader efter omstart. Gamla publicerade rader städas enligt retention av ett separat jobb.

Krasch mellan Kafka-ack och databasmarkering kan ge duplicat. Konsumenter måste därför använda `processed_events` i samma transaktion som sin affärseffekt.

## Konsekvenser

### Positiva

- Ingen domänändring tappas på grund av en dual write.
- Återstart och kontrollerad återpublicering är möjlig.
- Mönstret kan integrationstestas utan en separat CDC-plattform.
- Outboxstatus ger operativ insyn.

### Negativa och risker

- Polling ger viss latens och belastning på tjänstedatabasen.
- Publicering är at-least-once, inte exactly-once end-to-end.
- Låsning, batchstorlek, cleanup och poison rows måste övervakas.
- Payloaden dupliceras temporärt i domändata och outbox.

## Övervägda alternativ

- **Direkt Kafka-publicering efter commit:** enklare men kan tappa event vid krasch.
- **Kafka-publicering före commit:** kan exponera state som sedan rullas tillbaka.
- **Debezium CDC:** läser PostgreSQL WAL och publicerar outboxförändringar med låg latens. Det är ett kompatibelt framtida alternativ, men kräver Kafka Connect, connector-/offsetdrift och mer lokal infrastruktur.
- **XA/2PC:** avvisas på grund av komplexitet och sämre autonomi/tillgänglighet.

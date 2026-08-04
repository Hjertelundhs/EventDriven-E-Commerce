# Fas 4 — Inventory Service

**Status:** Pågående. Domän, REST-API, applikationslager, PostgreSQL-persistens och integrationstester är levererade. Fas 4 är inte komplett förrän idempotenta Kafka consumers, eventkontrakt och outboxpublicering är levererade.

## Levererat

- Självständig Java 21/Spring Boot 3.5-applikation med Maven Wrapper.
- Separat package root och bounded context utan delade Product-domänmodeller.
- Framework-fria `InventoryItem`- och `InventoryReservation`-modeller samt lokalt `Sku` value object.
- Domänoperationer för inleverans, justering, reservation, frisläppning och slutförande.
- Invarianter för positiva operationer, icke-negativa saldon och `totalQuantity = availableQuantity + reservedQuantity`.
- Hexagonala inbound/outbound-portar och transaktionell application service.
- REST API för saldo, inleverans, fysisk justering, reservation, frisläppning och slutförande.
- Stabilt reservation-ID, idempotenta repeat-anrop, ETags och RFC 9457-fel.
- JPA-adaptrar med `@Version`, explicit konfliktmappning och PostgreSQL/Flyway-schema.
- Databasconstraints som utgör sista skyddslinje för saldo- och reservationsinvarianter.
- Kafka producer/consumer-bas, observability, strukturerad loggning, health probes och OpenAPI.
- Multi-stage Dockerfile med icke-root-användare.
- Compose `apps`-profil på port 8083, Prometheus-target och lokal readiness/OpenAPI-verifiering.
- Domän-, application-, controller- och Compose-kontraktstest samt PostgreSQL Testcontainers-integrationstest.

## Databasgräns

Inventory Service använder endast `inventory_db` och principalen `inventory_app`. Reservationer refererar SKU och order-ID som externa identiteter men har inga foreign keys till Product- eller Order-databasen. Tabellen `processed_events` har sammansatt nyckel på consumer group och event-ID så att olika logiska consumers kan behandla samma event oberoende utan dubbel affärseffekt inom en consumer.

## Nästa increment i Fas 4

1. Definiera maskinvaliderbara JSON Schemas för inventory events.
2. Konsumera `OrderCreatedV1` och kompensationshändelser med atomisk processed-event-lagring.
3. Publicera `InventoryReservedV1`, `InventoryReservationFailedV1` och `InventoryReleasedV1` via transactional outbox.
4. Lägg till Kafka contract- och integrationstester för happy path, duplicate delivery och kompensation.

## Körning

```powershell
cd backend/inventory-service
.\mvnw.cmd verify
```

Hela lokala miljön startas från reporoten med `./scripts/start-local.ps1`.

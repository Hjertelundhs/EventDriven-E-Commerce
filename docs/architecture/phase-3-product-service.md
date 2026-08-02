# Fas 3 — Product Service

**Status:** Implementerad och verifierad 2026-08-02. Elva unit-/kontraktstester och den Docker-baserade Testcontainers-integrationen passerar. En ren Compose-start verifierar även databasgränser, Kafka, Redis, Keycloak, MailHog, Product Service och observability-stacken.

## Leverans

- Java 21 och Spring Boot 3.5 med Maven Wrapper.
- Framework-fri domänmodell för `Product`, `Sku` och `Money`.
- Inbound use-case-portar och outbound portar för persistence och outbox.
- REST API med allowlistade DTO:er, Bean Validation, pagination, sökning, filtrering och sortering.
- RFC 9457 Problem Details, korrelations-ID, ETag-respons och explicit optimistic concurrency via `version`.
- PostgreSQL-adapter med JPA, unika constraints, index, auditfält och Flyway.
- Redis-cache för produktläsningar och transaktionsmedveten invalidation vid mutation.
- `ProductChangedV1` med JSON Schema Draft 2020-12 och komplett event-envelope.
- Transactional Outbox med låst batch, producer retry/backoff och återstartssäker pending-state.
- Micrometer, Prometheus, OpenTelemetry/OTLP, strukturerad ECS-loggning och Actuator probes.
- Multi-stage Dockerfile med Java 21, icke-root-användare och container-healthcheck.
- Compose-profilen `apps`, Prometheus scrape target och utökad lokal verifiering.

## Lager och beroenden

```text
api -> application -> domain
             ^
             |
infrastructure adapters
```

`domain` importerar inga Spring-, JPA-, Kafka- eller Jackson-typer. Application-lagret äger use cases och portkontrakt. JPA, Redis, Kafka, HTTP och telemetry ligger i adapters/configuration och kan bytas utan att domänreglerna skrivs om.

## Affärsregler

- SKU normaliseras till versaler, har en strikt teckenuppsättning och är globalt unik i tjänsten.
- Pris är icke-negativt, har exakt två decimaler och en giltig ISO 4217-valuta.
- Namn och kategori krävs; alla längdgränser finns i både API, domän och databas där det är relevant.
- Avaktivering är idempotent och raderar aldrig historik.
- En avaktiverad produkt kan inte uppdateras.
- Stale `version` ger `409 Conflict`; JPA `@Version` skyddar även samtidiga transaktioner efter applikationskontrollen.

## Atomisk eventpublicering

`ProductApplicationService` kör mutation, productsave och outboxappend inom en Spring-transaktion. Eventet publiceras först efter commit av en separat publisher. PostgreSQLs `SKIP LOCKED` gör flera publisherinstanser säkra att köra parallellt. Producerkonfigurationen använder `acks=all` och idempotent Kafka-producer. Outboxmönstret garanterar at-least-once, inte exactly-once end-to-end.

## Testnivåer

| Nivå | Fokus |
|---|---|
| Domain unit | Invarianter, normalisering, avaktivering och otillåtna uppdateringar |
| Application unit | Duplicate SKU, optimistic version och eventskapande |
| Controller | HTTP-status, Location, ETag, correlation och Problem Details |
| Contract | Envelope och koppling till versionerat JSON Schema |
| Integration | PostgreSQL/Flyway, Redis, Kafka, REST Assured och faktisk outboxpublicering |

## Nästa fas

Fas 4 implementerar Inventory Service med lagersaldo, inleverans, justering, reservation, release, slutförande, optimistic locking, idempotent Kafka consumption, outbox och tester.

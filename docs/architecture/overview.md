# Arkitekturöversikt

## Systemkontext

Plattformen stödjer produktvisning, checkout, order, betalning, lagerreservation, leveransspårning och notifieringar. Kunder, administratörer, support och lagerpersonal använder samma webbapplikation men får olika funktioner genom rollerna `CUSTOMER`, `ADMIN`, `SUPPORT` och `WAREHOUSE`.

Systemet delas i bounded contexts som speglar affärsförmågor. Varje tjänst äger sin modell, sina regler och sin databas. En tjänst får aldrig fråga en annan tjänsts databas eller skapa främmande nycklar över tjänstegränser. Referenser mellan kontexter lagras som stabila identifierare och verifieras via API eller händelser när det behövs.

## Arkitekturstil

Varje backendtjänst blir en separat Java 21/Spring Boot 3-applikation med hexagonal arkitektur:

```text
api/inbound adapters -> application/use cases -> domain
infrastructure/outbound adapters -> application ports <- domain
configuration wires adapters and ports
```

- `domain` innehåller aggregates, entities, value objects, domäntjänster, regler och domänhändelser. Lagret har inga ramverksberoenden.
- `application` innehåller commands, queries, use cases, transaktionsgränser och portar.
- `api` innehåller REST/Kafka-inbound adapters, DTO:er, validering och felöversättning.
- `infrastructure` innehåller JPA, Kafka, Redis, externa providers och outbox-adaptrar.
- `configuration` kopplar samman portar och adaptrar samt säkerhet och observability.

Domänmodeller delas inte mellan tjänster. `shared-contracts` får endast innehålla versionshanterade, tekniska eventkontrakt och tillhörande kompatibilitetsartefakter.

## Kommunikation

REST används när anroparen behöver ett omedelbart svar, exempelvis produktfrågor, orderregistrering, orderläsning och leveransspårning. Kafka används för domänhändelser, saga-steg, cacheinvalidering, notifieringar och auditströmning. Kärnflödet efter accepterad order är asynkront och fortsätter även om klienten kopplar ned.

Leveransgarantin är at-least-once. Exakt en affärseffekt uppnås därför genom idempotenta konsumenter, unika affärsnycklar, optimistic locking och tabellen `processed_events`. Producenter använder transactional outbox: aggregateändring och outboxrad sparas i samma lokala databastransaktion och publiceras senare av en återstartbar publisher.

## Dataägarskap

Varje deploybar tjänst får ett separat PostgreSQL-schema eller, lokalt, en separat databas i samma PostgreSQL-instans. Produktionsmiljön separerar användare och rättigheter per databas. API Gateway behåller endast gatewayrelaterad säkerhets-/auditmetadata i sin databas och är i övrigt stateless. Identity Service lagrar användarprofil och preferenser; Keycloak äger credentials, sessioner och identitetsdata.

Redis används för produktcache, distribuerad rate limiting och kortlivad token-/sessionsmetadata. Redis är aldrig system of record. Cacheposter har TTL, namespacade nycklar och invalideras av versionshanterade produktändringshändelser.

## Säkerhetsmodell

Keycloak är OpenID Provider. Webbläsaren använder Authorization Code Flow med PKCE. Gatewayn validerar issuer, audience, signatur och tidsanspråk innan routing. Varje berörd tjänst validerar JWT på nytt och använder method-level security; gatewaykontroll är inte den enda säkerhetsgränsen.

Orderägarskap kontrolleras mot det autentiserade subjektet, inte ett fritt `customerId` från requesten. Privilegierade operationer kräver explicita roller. Tokens, lösenord och betalningsdata får inte loggas. Payment Service lagrar endast simulerade providerreferenser, maskerade metadata och auditposter, aldrig kortuppgifter.

## Resiliens

- Timeout och circuit breaker skyddar synkrona externa beroenden.
- Retry används enbart för klassificerade transienta fel, med exponentiell backoff och jitter.
- Bulkhead isolerar långsamma providers och rate limiting tillämpas i gatewayn.
- Kafka-fel går genom begränsade retries till versions-/domänspecifik dead-letter topic.
- Poison messages återspelas kontrollerat; de hoppas inte över tyst.
- Optimistic locking skyddar lager och orderstate från lost updates.

## Observability

W3C Trace Context används över HTTP och propagateras i Kafka-headers. `correlationId` följer hela affärsflödet och `causationId` binder en händelse till sin utlösare. JSON-loggar innehåller service, environment, traceId, spanId och correlationId men inga känsliga värden.

Spring Boot Actuator och Micrometer exponerar health, readiness, liveness och Prometheus-metrics. OpenTelemetry exporterar traces till Tempo, loggar samlas i Loki och Grafana korrelerar logs, metrics och traces.

## Viktiga kvalitetsattribut

| Attribut | Arkitekturellt svar |
|---|---|
| Tillgänglighet | Asynkron saga, återstartbar outbox, probes och isolerade fel |
| Konsistens | Lokal ACID, eventual consistency över tjänster, explicita kompensationer |
| Skalbarhet | Stateless API-noder, Kafka-partitionering per aggregate, oberoende deployment |
| Säkerhet | OIDC/PKCE, JWT defense in depth, least privilege och audit |
| Ändringsbarhet | Bounded contexts, hexagonala portar, versionshanterade kontrakt |
| Spårbarhet | Correlation/causation IDs, audit, metrics, logs och traces |

## Datakonsistens och outbox

En serviceoperation öppnar en lokal transaktion, ändrar aggregatet och infogar eventets envelope i `outbox_events`. Publishern låser en begränsad batch, publicerar med eventets `eventId` och markerar raden som publicerad. Om processen kraschar före markeringen publiceras eventet igen; konsumentens `processed_events` och affärsunikhet gör dupliceringen ofarlig.

Debezium CDC är ett framtida alternativ där WAL-förändringar från outbox-tabellen strömmas utan applikationspolling. Det minskar publisherkod och latens men ökar plattformsdrift, connector-livscykel och schemahantering. För portfolioets första version väljs en explicit polling publisher som är enklare att köra lokalt och testa deterministiskt.

## Vidare läsning

- [Tjänsteansvar](service-responsibilities.md)
- [Kommunikationsflöden](communication-flows.md)
- [Order-saga](order-saga.md)
- [Diagram](diagrams.md)
- [Eventkatalog](../events/event-catalog.md)
- [Architecture Decision Records](../adr/README.md)

# Product Service

Product Service äger produktkatalogen och databasen `product_db`. Tjänsten är en Java 21/Spring Boot 3-applikation med hexagonal lagerindelning, PostgreSQL/Flyway, Redis-cache, OpenAPI och en transaktionell outbox som publicerar `ProductChangedV1` till Kafka.

## Bygg och testa

Maven Wrapper laddar automatiskt ned Maven 3.9.11. JDK 21 rekommenderas och är den version som används i containerbygget.

```powershell
cd backend/product-service
.\mvnw.cmd verify
```

`verify` kör unit-, application-, controller- och kontraktstester. Integrationstestet startar PostgreSQL, Redis och Kafka med Testcontainers när Docker är tillgängligt; utan Docker markeras det som skipped.

## Lokal start med Compose

Från repositoryroten:

```powershell
Copy-Item .env.example .env
# Ersätt alla CHANGE_ME-värden.
.\scripts\start-local.ps1
```

Product Service exponeras på `http://127.0.0.1:8082`. Swagger UI finns på `http://127.0.0.1:8082/swagger-ui.html`, OpenAPI på `/v3/api-docs`, readiness på `/actuator/health/readiness` och Prometheus-metrics på `/actuator/prometheus`.

## API

| Metod | Endpoint | Funktion |
|---|---|---|
| `POST` | `/api/v1/products` | Skapa produkt |
| `PUT` | `/api/v1/products/{id}` | Uppdatera produkt med expected `version` |
| `DELETE` | `/api/v1/products/{id}?version=n` | Avaktivera produkt |
| `GET` | `/api/v1/products/{id}` | Hämta produkt |
| `GET` | `/api/v1/products` | Sök, filtrera, sortera och paginera |

Exempel:

```powershell
$body = @{
    sku = 'KEYBOARD-001'
    name = 'Mechanical Keyboard'
    description = 'Hot-swappable mechanical keyboard'
    category = 'Peripherals'
    price = 1499.00
    currency = 'SEK'
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
    -Uri http://127.0.0.1:8082/api/v1/products `
    -ContentType application/json `
    -Body $body
```

Listning använder `page`, `size`, `sort`, `direction`, `name`, `category` och `active`. Tillåtna sortfält är `name`, `category`, `price`, `created_at` och `updated_at`. API:t returnerar RFC 9457 Problem Details vid fel och `X-Correlation-ID` i varje svar.

## Konsistens och events

Produktändringen och hela event-envelope sparas i samma databastransaktion. En schemalagd publisher låser en begränsad batch med `FOR UPDATE SKIP LOCKED`, publicerar med produkt-ID som Kafka-nyckel och markerar eventet `PUBLISHED`. Vid fel ligger posten kvar som `PENDING` med exponentiell backoff. En krasch efter Kafka-ack men före databascommit kan ge omleverans; framtida konsumenter måste därför vara idempotenta.

JSON Schema finns i `backend/shared-contracts/events/product-changed-v1.schema.json`. Outbox-tabellen kan senare ersättas eller kompletteras med Debezium CDC utan att domän- eller API-lagret ändras.

## Kända begränsningar

- ADMIN-auktorisering för skrivoperationer införs i Fas 8 tillsammans med Gateway och JWT-validering.
- Contains-sökningen använder portabel SQL `LIKE`; fulltextsökning och `pg_trgm` är avsiktligt inte introducerade än.
- Docker krävs för Testcontainers, Compose-verifiering och end-to-end-test av Kafka-publicering.
- Lokal Redis och Kafka kör single-node och är inte en produktions-HA-konfiguration.

# API-principer

Detta dokument är ett arkitekturkontrakt för kommande REST-API:er, inte en genererad OpenAPI-specifikation.

## Basregler

- Publika endpoints versioneras under `/api/v1`.
- Resurser namnges som plural substantiv; commands används som underresurser när en ren CRUD-operation inte uttrycker avsikten.
- Request-DTO:er är explicita allowlists och mappas till commands; persistence-entiteter bindas aldrig direkt.
- Datum/tid använder RFC 3339 UTC och pengar representeras som decimalsträng + ISO 4217-valuta.
- Klientgenererade `customerId`, status, totalpris, roller och auditfält ignoreras inte utan avvisas om de inte ingår i kontraktet.

## Centrala resurser

| Metod och path | Syfte | Primär behörighet |
|---|---|---|
| `GET /api/v1/products` | Sök, filter, sortering och pagination | Publik/autentiserad |
| `GET /api/v1/products/{productId}` | Produktdetalj | Publik/autentiserad |
| `POST /api/v1/products` | Skapa produkt | `ADMIN` |
| `PUT /api/v1/products/{productId}` | Uppdatera produkt | `ADMIN` |
| `POST /api/v1/orders` | Registrera idempotent order | `CUSTOMER` |
| `GET /api/v1/orders/{orderId}` | Läs order med ägarskapskontroll | Ägare, `ADMIN`, `SUPPORT` |
| `GET /api/v1/customers/me/orders` | Egen orderhistorik | `CUSTOMER` |
| `GET /api/v1/orders/{orderId}/events` | SSE för status | Ägare, `ADMIN`, `SUPPORT` |
| `GET /api/v1/deliveries/{trackingNumber}` | Leveransspårning | Ägare, `ADMIN`, `SUPPORT` |
| `POST /api/v1/inventory/{sku}/adjustments` | Justera lager | `WAREHOUSE`, `ADMIN` |

## Statuskoder

- `200 OK` för lyckad läsning/uppdatering med response body.
- `201 Created` för synkront skapad administrativ resurs med `Location`.
- `202 Accepted` när orderflödet accepterats och fortsätter asynkront.
- `204 No Content` för lyckad operation utan representation.
- `400 Bad Request` för syntax eller format, `401` för saknad/ogiltig authentication, `403` för nekad behörighet.
- `404 Not Found` används även när resursens existens inte får avslöjas för annan kund.
- `409 Conflict` för state-/optimistic-locking-/idempotency-konflikt.
- `422 Unprocessable Content` för semantiskt ogiltig input som klarat strukturell validering.
- `429 Too Many Requests` med `Retry-After` för rate limiting.
- `503 Service Unavailable` för tillfälligt otillgängligt obligatoriskt beroende.

## Problem Details

Fel använder `application/problem+json` enligt RFC 9457:

```json
{
  "type": "https://errors.example.test/order/invalid-state",
  "title": "Order state transition is not allowed",
  "status": 409,
  "detail": "The requested operation cannot be applied in the current state.",
  "instance": "/api/v1/orders/018f5f5b-d133-76b8-a57c-f1618d74c45e",
  "correlationId": "018f5f58-e584-7b04-b9ac-2e5c7d6e9f70"
}
```

`detail` innehåller aldrig stack trace, tokens, SQL eller känsliga attribut. Valideringsfel läggs i en `violations`-array med stabil field path och reason code.

## Pagination och sortering

Collection endpoints använder `page` (0-baserad), `size` med ett serverdefinierat max, och en allowlistad `sort=field,direction`. Svaret innehåller `items` och metadata: `page`, `size`, `totalElements`, `totalPages`, `first`, `last`. Stora eller föränderliga auditflöden kan senare få cursor-pagination genom ett separat kontrakt.

## Idempotens och concurrency

`POST /api/v1/orders` och alla betalnings-/refundkommandon kräver `Idempotency-Key`. Servern binder nyckeln till autentiserat subjekt, endpoint och hash av normaliserad request. Samma nyckel + samma request returnerar samma resultat; samma nyckel + annan request ger `409`.

Uppdateringar av concurrencysensitiva resurser använder `ETag`/`If-Match` eller ett explicit versionsfält. En stale version ger `409` eller `412 Precondition Failed` enligt endpointens publicerade OpenAPI-kontrakt.

## Correlation och tracing

Gatewayn accepterar inte blint klientens interna headers. Den validerar eller skapar `X-Correlation-ID`, returnerar värdet i response och propagerar det. W3C `traceparent` används för tracing. Affärsidempotens får aldrig baseras på trace-ID.

## OpenAPI

Varje tjänst äger sin OpenAPI 3.1-specifikation. Gatewayn kan samla länkar men äger inte downstream-kontrakten. CI validerar specs, upptäcker brytande ändringar och genererar inte domänmodeller som delas mellan tjänster.

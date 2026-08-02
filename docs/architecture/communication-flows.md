# Kommunikationsflöden

## Principer

1. Klienttrafik går via API Gateway; interna tjänster exponeras inte publikt.
2. REST används endast när ett omedelbart svar eller aktuell validering krävs.
3. Kafka används när avsändaren inte ska blockera på downstream-arbete eller när flera konsumenter behöver samma affärsfakta.
4. Ingen kedja av synkrona anrop används för själva sagan efter att ordern accepterats.
5. HTTP och Kafka bär W3C trace context, `correlationId` och teknisk message metadata.

## Synkrona flöden

| Från | Till | Flöde | Motiv och felbeteende |
|---|---|---|---|
| Webbläsare | Keycloak | OIDC Authorization Code + PKCE | Interaktiv login; credentials passerar aldrig plattformens tjänster |
| Webbläsare | API Gateway | Alla `/api/v1/**`-anrop | Gemensam TLS-terminering, rate limiting och routing |
| API Gateway | Product Service | Lista, sök, filtrera och administrera produkt | Omedelbar katalogrespons; cache kan användas för läsning |
| API Gateway | Order Service | Registrera/läsa order och SSE-status | Kommandot returnerar `202 Accepted` när order + outbox är lagrad |
| Order Service | Product Service | Validera aktiv SKU och hämta auktoritativ pris-snapshot vid checkout | Kort timeout; order skapas inte vid fel eller inaktiv produkt; circuit breaker skyddar tjänsten |
| API Gateway | Inventory Service | Lageradministration | Endast `WAREHOUSE`/`ADMIN`; optimistic locking-konflikt ger `409` |
| API Gateway | Delivery Service | Slå upp tracking och administrera status | Ägarskap/roll kontrolleras i tjänsten |
| API Gateway | Identity Service | Profil och notifieringspreferenser | OIDC `sub` härleds från token |
| Notification Service | MailHog | SMTP | Asynkront jobb med begränsad retry; påverkar inte saga |

Payment-provideradapter är lokal och simulerad i första versionen. En verklig provider skulle vara ett synkront outbound-anrop bakom timeout, bulkhead, circuit breaker och provider-idempotency key.

## Asynkrona flöden

| Producent | Händelsegrupp | Primära konsumenter | Syfte |
|---|---|---|---|
| Order Service | order lifecycle | Inventory, Payment, Delivery, Notification, auditprojektion | Starta saga, förse lokala väntande projektioner och kommunicera slutstatus |
| Inventory Service | inventory reservation | Order, Payment, Notification | Fortsätta eller kompensera saga |
| Payment Service | payment/refund | Order, Delivery, Notification | Fortsätta saga eller signalera kompensation |
| Delivery Service | delivery lifecycle | Order, Payment, Inventory, Notification | Spårning, slutförande eller kompensation |
| Product Service | product lifecycle | cache consumers/read models | Invalidering och katalogprojektion |

Exakta topics, keys och händelser finns i [eventkatalogen](../events/event-catalog.md).

## Realtidsstatus

Frontend prenumererar på `GET /api/v1/orders/{orderId}/events` via Server-Sent Events. Order Service läser inte Kafka direkt för varje klient utan publicerar uppdateringar från sin egen orderstatusprojektion. SSE väljs framför WebSocket eftersom flödet är enkelriktat, återanslutning stöds och standard-HTTP-infrastruktur kan användas. `Last-Event-ID` möjliggör kort återhämtning; efter längre avbrott hämtar klienten aktuell order via REST.

## Felkontrakt

REST-fel följer RFC 9457 Problem Details och innehåller `type`, `title`, `status`, `detail`, `instance`, `correlationId` och valideringsfel utan känsliga data. Kafka-konsumenter klassificerar fel som:

- **Transienta:** timeout, tillfällig broker-/databasstörning; retry med backoff och jitter.
- **Permanenta kontraktsfel:** ogiltigt schema/version; DLT och alarm.
- **Affärsutfall:** exempelvis otillräckligt lager; normal domänhändelse, inte teknisk retry.
- **Duplicat:** kvitteras efter verifierad `processed_events`-post och ger ingen ny affärseffekt.

## Tillitsgränser

- Gatewayn sanerar inkommande forwarding- och correlationheaders och skapar egna om de saknas.
- Tjänster litar på signerad JWT, inte godtyckliga `X-User-*` headers.
- Kafka ACL begränsar varje service account till nödvändiga topics och consumer groups.
- Intern REST använder i Kubernetes nätverkspolicy och separat serviceidentitet; mTLS kan läggas till utan att ändra domänlagret.

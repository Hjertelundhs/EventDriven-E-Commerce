# Tjänsteansvar

## API Gateway

**Äger:** extern API-ingång, routing, CORS, säkra headers, distribuerad rate limiting, tokenvalidering, correlation ID och gatewayaudit.

**Gör inte:** affärslogik, domänaggregering eller behörighetskontroll som endast finns i gatewayn. Gatewayn vidarebefordrar verifierad användarkontext men downstream-tjänsten fattar det slutliga auktoriseringsbeslutet.

**Data:** `gateway_db` för durable säkerhets-/auditmetadata; Redis för rate-limit counters och kortlivad metadata.

## Identity Service

**Äger:** applikationsprofil, kundkontaktuppgifter, notifieringspreferenser, samtycken och en administrativ facade mot identitetsfunktioner.

**Gör inte:** lagrar lösenord, utfärdar tokens eller duplicerar Keycloaks användar-/sessionsmodell. Keycloak är ensam identity provider.

**Data:** `identity_db`. Refererar Keycloak-användare via OIDC `sub`.

## Product Service

**Äger:** produktaggregatet, SKU, namn, beskrivning, kategori, pris, valuta, aktiv-status och katalogsökning.

**Publicerar:** produkt skapad/uppdaterad/avaktiverad för cacheinvalidering och framtida read models.

**Gör inte:** lagerstatus, reservationer eller orderrader. En order lagrar en oföränderlig snapshot av namn och pris vid köp.

**Data:** `product_db`; Redis är cache, aldrig system of record.

## Inventory Service

**Äger:** lagersaldo per SKU, inleverans, justering, reservation, frisläppning och slutförande. Upprätthåller `total = available + reserved`, förbjuder negativt saldo och använder optimistic locking.

**Konsumerar:** order- och kompensationshändelser.

**Publicerar:** reservation lyckad/misslyckad samt reservation släppt/slutförd.

**Data:** `inventory_db` med unika reservationer per order och SKU.

## Order Service

**Äger:** orderaggregatet, orderrader, pris-snapshot, adresser, total, valuta, kundägarskap och order-state machine. Det är system of record för kundens orderstatus och exponerar realtidsström för orderuppdateringar.

**Konsumerar:** lager-, betalnings- och leveranshändelser.

**Publicerar:** skapad, slutförd och avbruten order. Håller en saga-progressprojektion för att kunna visa status och avgöra när kompensationer är klara; den skickar inte centraliserade stegkommandon.

**Data:** `order_db`. `Idempotency-Key` är unik per kund och requestfingerprint.

## Payment Service

**Äger:** payment-aggregatet, authorization/capture, idempotency, providerreferenser, betalningshistorik, återbetalningsstatus och betalningsaudit.

**Konsumerar:** `OrderCreatedV1` för att lagra belopp/valuta i en lokal väntande betalningsprojektion, `InventoryReservedV1` för att genomföra betalningen och `DeliveryFailedV1` för kompensation.

**Publicerar:** betalning klar/misslyckad samt refund begärd/slutförd/misslyckad.

**Gör inte:** lagrar riktiga kortuppgifter. Simuleringsinstruktioner är testmetadata och får inte sammanblandas med PAN/CVV.

**Data:** `payment_db`, krypterade eller maskerade icke-känsliga providerreferenser.

## Delivery Service

**Äger:** leveransaggregatet, trackingnummer, estimerat leveransdatum, status och statushistorik. Ett schemalagt jobb simulerar tillåtna statusövergångar.

**Konsumerar:** `OrderCreatedV1` för minsta nödvändiga fulfillmentdata och `PaymentCompletedV1` för att skapa leveransen.

**Publicerar:** leverans skapad/misslyckad och leveransstatus ändrad.

**Data:** `delivery_db`; unik leverans per order och unikt trackingnummer.

## Notification Service

**Äger:** notifieringsjobb, mallreferens, kanal, leveransförsök och slutstatus. Konsumerar relevanta domänhändelser och skickar simulerad e-post via MailHog.

**Gör inte:** påverkar sagans affärsutfall. Ett notifieringsfel får inte rulla tillbaka ordern.

**Data:** `notification_db`, med unik kombination av event, mottagare, kanal och mall för idempotens.

## Shared Contracts

**Äger:** stabila event-envelope-scheman, versionsspecifika JSON Schema-filer, exempel och kompatibilitetstester.

**Gör inte:** innehåller gemensamma aggregates, entities, repositories, servicebas-klasser eller JPA-mappningar. Varje tjänst mappar kontrakt till sin egen domän.

## Plattformskomponenter

| Komponent | Ansvar | System of record? |
|---|---|---|
| Keycloak | Identity provider, realm, clients, roller och tokenutfärdning | Ja, för identitet |
| Kafka | Händelsetransport och replay inom retention | Nej |
| Redis | Cache, rate limiting och kortlivad metadata | Nej |
| MailHog | Lokal simulerad SMTP-provider | Nej |
| Prometheus | Scrape och tidsseriemetrics | Ja, för metricsretention |
| Loki | Centraliserade loggar | Ja, för loggretention |
| Tempo | Distribuerade traces | Ja, för traceretention |
| Grafana | Visualisering och korrelation | Nej |

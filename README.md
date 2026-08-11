# Order & Logistics Platform

Ett produktionsnära portfolio-projekt för en eventdriven order- och logistikplattform. Monorepot byggs iterativt i tio faser. Fas 1–5 är levererade och Fas 6 har en körbar Payment Service-baslinje.

## Arkitekturella mål

- Självständigt deploybara tjänster med tydliga bounded contexts och databasägarskap.
- Hexagonal arkitektur där domänen är fri från Spring, JPA och transportdetaljer.
- REST för frågor och omedelbar validering, Kafka för domänhändelser och långlivade arbetsflöden.
- Choreography-baserad saga med idempotenta kompensationer.
- Transactional outbox för atomisk lagring av domänändring och händelse.
- Defense in depth med Keycloak, JWT-validering i gateway och tjänster samt metodbehörighet.
- Observability från början: korrelation, strukturerade loggar, metrics och distribuerad tracing.

## Fas 1-dokumentation

- [Arkitekturöversikt](docs/architecture/overview.md)
- [Projektstruktur](docs/architecture/project-structure.md)
- [Tjänsteansvar](docs/architecture/service-responsibilities.md)
- [Kommunikationsflöden](docs/architecture/communication-flows.md)
- [Saga och orderflöde](docs/architecture/order-saga.md)
- [Diagram](docs/architecture/diagrams.md)
- [Eventkatalog](docs/events/event-catalog.md)
- [API-principer](docs/api/api-principles.md)
- [ADR-index](docs/adr/README.md)
- [Plan för Fas 2](docs/architecture/phase-2-plan.md)
- [Lokal utvecklingsguide](docs/architecture/local-development.md)
- [Fas 3 — Product Service](docs/architecture/phase-3-product-service.md)
- [Fas 4 — Inventory Service](docs/architecture/phase-4-inventory-service.md)
- [Fas 5 — Order Service](backend/order-service/README.md)
- [Fas 6 — Payment Service](backend/payment-service/README.md)
- [Teststrategi](docs/testing/test-strategy.md)

## Lokal start

Installera Docker Desktop, skapa en lokal miljöfil och ersätt samtliga `CHANGE_ME`-värden:

```powershell
Copy-Item .env.example .env
notepad .env
.\scripts\start-local.ps1
```

Skriptet startar PostgreSQL, Kafka, Redis, Keycloak, MailHog, Product Service, Inventory Service, Order Service, Payment Service, Kafka UI och observability-stacken och kör därefter verifiering. Fullständig port-, credential- och felsökningsinformation finns i [lokal utvecklingsguide](docs/architecture/local-development.md).

Product Service kan även byggas separat:

```powershell
cd backend/product-service
.\mvnw.cmd verify
```

Inventory Service byggs och integrationstestas separat med:

```powershell
cd backend/inventory-service
.\mvnw.cmd verify
```

Order Service byggs och integrationstestas separat med:

```powershell
cd backend/order-service
.\mvnw.cmd -Djacoco.skip=true verify
```

Payment Service verifieras separat med:

```powershell
cd backend/payment-service
.\mvnw.cmd verify
```

## Kända begränsningar i pågående Fas 6

- Product, Inventory och Order Service driver nu order-till-lager-sagan via Kafka och transactional outbox.
- Payment Service har nu den eventdrivna betalnings- och refundbaslinjen. Delivery, Notification, Gateway och React-applikationen följer i senare faser.
- API-skrivningar är inte ännu rollskyddade; defense-in-depth med Gateway, JWT och method security levereras i Fas 8. Order Service använder tills dess den explicita trust boundary-headern `X-Customer-ID`.
- Product-, Order-, Inventory- och Payment-händelser har maskinvaliderbara JSON Scheman.
- Den lokala infrastrukturen är single-node och använder loopbackbunden plaintext där produktion kräver TLS och redundans.
- Kubernetes, Helm, Terraform, nätverkspolicyer och produktionssecrets tillhör Fas 10.

## Beslutsdisciplin

Arkitekturbeslut dokumenteras som ADR:er. Ändringar görs genom en ny ADR som ersätter ett tidigare beslut i stället för att historiken skrivs om.

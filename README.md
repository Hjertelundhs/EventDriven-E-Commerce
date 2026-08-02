# Order & Logistics Platform

Ett produktionsnära portfolio-projekt för en eventdriven order- och logistikplattform. Monorepot byggs iterativt i tio faser. **Fas 1: arkitektur** och **Fas 2: lokal infrastruktur** är levererade. Ingen backend- eller frontendapplikation har genererats ännu.

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

## Lokal start

Installera Docker Desktop, skapa en lokal miljöfil och ersätt samtliga `CHANGE_ME`-värden:

```powershell
Copy-Item .env.example .env
notepad .env
.\scripts\start-local.ps1
```

Skriptet startar PostgreSQL, Kafka, Redis, Keycloak, MailHog, Kafka UI och observability-stacken och kör därefter verifiering. Fullständig port-, credential- och felsökningsinformation finns i [lokal utvecklingsguide](docs/architecture/local-development.md).

## Kända begränsningar efter Fas 2

- Inga Spring Boot-applikationer, React-filer, databasmigrationer eller tester finns ännu.
- Eventkatalogen definierar semantiken; maskinvaliderbara JSON Schema-filer skapas tillsammans med första producerande tjänsten.
- Den lokala infrastrukturen är single-node och använder loopbackbunden plaintext där produktion kräver TLS och redundans.
- Kubernetes, Helm, Terraform, nätverkspolicyer och produktionssecrets tillhör Fas 10.

## Beslutsdisciplin

Arkitekturbeslut dokumenteras som ADR:er. Ändringar görs genom en ny ADR som ersätter ett tidigare beslut i stället för att historiken skrivs om.

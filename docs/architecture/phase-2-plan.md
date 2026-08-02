# Fas 2 — lokal infrastruktur

**Status:** Genomförd 2026-08-02. Den verifierbara leveransen beskrivs i [lokal utvecklingsguide](local-development.md).

## Mål

Fas 2 ska ge en reproducerbar, säker lokal utvecklingsmiljö för plattformens externa beroenden. Efter fasen ska en utvecklare kunna starta infrastrukturen, verifiera health och ansluta kommande tjänster utan lokalt installerad PostgreSQL, Kafka, Redis, Keycloak, MailHog eller observability-stack.

Eftersom applikationstjänsterna implementeras i Fas 3–8 ska Fas 2 inte skapa falska backendcontainrar. `docker-compose.yml` blir infrastrukturnav och kompletteras med respektive appservice när dess körbara kod och Dockerfile levereras. När Fas 9 är klar startar samma composefil hela plattformen.

## Levererade huvudfiler

```text
.
├── .env.example
├── docker-compose.yml
├── infrastructure/
│   ├── docker/
│   │   ├── postgres/init-databases.sh
│   │   └── README.md
│   ├── keycloak/
│   │   ├── realm-export.json
│   │   └── bootstrap-users.sh
│   ├── messaging/
│   │   ├── create-topics.sh
│   │   └── README.md
│   └── monitoring/
│       ├── prometheus/prometheus.yml
│       ├── grafana/provisioning/datasources/datasources.yml
│       ├── grafana/provisioning/dashboards/dashboards.yml
│       ├── grafana/dashboards/platform-overview.json
│       ├── loki/loki-config.yml
│       ├── tempo/tempo.yml
│       └── alloy/config.alloy
└── scripts/
    ├── start-local.ps1
    ├── stop-local.ps1
    └── verify-local.ps1
```

Images är låsta till explicita versionstaggar och inga `latest`-taggar används. Digest-pinning införs med imagebyggande och security scanning i Fas 10.

## Arbetsordning

1. **Compose-bas:** nätverk, namngivna volymer, healthchecks, restart-policyer, resource hints och profiler.
2. **PostgreSQL:** en lokal instans med separata databaser och principals för gateway, identity, product, inventory, order, payment, delivery och notification. Init-skript är idempotent för en ny volym. Applikationskonton får inte superuser-rättighet.
3. **Kafka:** KRaft-baserad enkel broker för lokal utveckling, explicit listeners, Kafka UI och init-jobb för topics/DLTs. Auto topic creation stängs av.
4. **Redis:** persistence lämplig för lokal miljö, healthcheck och autentisering via environmentvariabel.
5. **Keycloak:** importerbart realm, SPA-client med PKCE, gateway/service audiences, rollerna `CUSTOMER`, `ADMIN`, `SUPPORT`, `WAREHOUSE` och lokala exempelanvändare vars lösenord kommer från `.env`/bootstrapvariabler.
6. **MailHog:** SMTP och webb-UI på dokumenterade portar, inga riktiga externa utskick.
7. **Observability:** Prometheus, Grafana, Loki och Tempo med provisionerade datasources och en dashboard som redan har paneldefinitioner för kommande metrics.
8. **Verifiering:** script som väntar på health, kontrollerar topics, databaser, Keycloak discovery, Redis, MailHog och monitoring endpoints.
9. **Dokumentation:** portmatris, credentialsflöde, reset/backup-begränsningar, felsökning och exakta start/stopp-kommandon.

## Compose-profiler

| Profil | Innehåll | Avsikt |
|---|---|---|
| `core` | PostgreSQL, Kafka, topic-init, Redis, Keycloak, MailHog | Minsta miljö för tjänsteutveckling |
| `tools` | Kafka UI | Valfri lokal inspektion |
| `observability` | Prometheus, Grafana, Loki, Tempo | Telemetri och dashboards |
| `apps` | Läggs till inkrementellt i Fas 3–9 | Körbara plattformstjänster och frontend |

## Säkerhetsregler

- `.env` ignoreras av Git; endast `.env.example` med platshållare versionshanteras.
- Compose kräver att känsliga variabler sätts och använder inga produktionshemligheter.
- Databasprincipals och Kafkaåtkomst separeras där lokala images stödjer det utan oproportionerlig komplexitet.
- Administrationsportar binds till localhost och exponeras inte på LAN.
- Keycloak använder testdata tydligt märkt för lokal miljö; importen får inte användas i produktion.
- Loggkonfiguration redigerar authorizationheaders och secrets.

## Health och beroenden

`depends_on` med service health används endast för startup-ordning, inte som applikationsresiliens. Varje komponent får en faktisk funktionskontroll: `pg_isready`, Kafka metadata/topic check, Redis `PING`, Keycloak health/discovery och HTTP-health för UI/telemetrykomponenter.

## Start och verifiering efter Fas 2

```powershell
Copy-Item .env.example .env
# Fyll lokala värden i .env
docker compose --profile core --profile tools --profile observability up -d
.\scripts\verify-local.ps1
```

Stopp utan databorttagning:

```powershell
docker compose down
```

Radering av lokala volymer dokumenteras som en separat, uttryckligen destruktiv operation och körs aldrig av standardskriptet.

## Acceptanskriterier

- `docker compose config` lyckas utan odefinierade obligatoriska variabler efter att `.env.example` kopierats och fyllts.
- Alla valda containrar blir healthy inom dokumenterad tid.
- Åtta tjänstedatabaser och Keycloaks separata databas kan nås endast med respektive lokala principal.
- Alla katalogiserade topics och DLTs finns med explicit partition/retention-konfiguration.
- OIDC discovery fungerar och testtoken innehåller rätt issuer, audience och realm roles.
- Redis kräver autentisering och svarar på healthcheck.
- Ett testmeddelande syns i MailHog utan extern leverans.
- Grafana har provisionerade Prometheus-, Loki- och Tempo-datasources.
- Inga hemligheter, genererade volymer eller credentials har lagts i Git.
- En ny miljö kan startas och verifieras med dokumenterade kommandon.

## Kända Fas 2-begränsningar

- Single-node Kafka/PostgreSQL/Redis demonstrerar integration, inte hög tillgänglighet.
- TLS kan termineras lokalt först när gatewayn introduceras; interna lokala länkar binds till ett privat compose-nätverk.
- Dashboardpaneler saknar applikationsdata tills respektive tjänst exponerar metrics.
- Kubernetes, Helm, Azure och produktionssecret management tillhör Fas 10.

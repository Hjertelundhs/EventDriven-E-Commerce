# Lokal utvecklingsmiljö

## Förutsättningar

- Docker Desktop med Docker Compose-plugin.
- PowerShell 5.1 eller senare.
- Minst cirka 6 GB ledigt container-minne för hela miljön.
- Portarna i `.env` måste vara lediga på `127.0.0.1`.

Ingen lokal Java-, Maven-, Node-, PostgreSQL-, Kafka- eller Redis-installation behövs för Compose-start. Product Service byggs i container med Java 21 och Maven 3.9.11.

## Låsta komponentversioner

| Komponent | Container image |
|---|---|
| PostgreSQL | `postgres:17.9-alpine` |
| Redis | `redis:8.8.0-alpine` |
| Kafka | `apache/kafka:4.3.1` |
| Kafka UI | `provectuslabs/kafka-ui:v0.7.2` |
| Keycloak | `quay.io/keycloak/keycloak:26.7.0` |
| MailHog | `mailhog/mailhog:v1.0.1` |
| Prometheus | `prom/prometheus:v3.12.0` |
| Grafana | `grafana/grafana:13.0.1-security-01` |
| Loki | `grafana/loki:3.6.12` |
| Tempo | `grafana/tempo:3.0.2` |
| Grafana Alloy | `grafana/alloy:v1.18.0` |
| Kafka Exporter | `danielqsj/kafka-exporter:v1.9.0` |
| Redis Exporter | `oliver006/redis_exporter:v1.77.0` |
| PostgreSQL Exporter | `prometheuscommunity/postgres-exporter:v0.17.1` |

## Första start

Skapa din lokala miljöfil och ersätt samtliga `CHANGE_ME`-värden med unika värden på minst 16 tecken. Tillåtna tecken är bokstäver, siffror, punkt, understreck, tilde och bindestreck så att Docker `.env`-tolkning förblir entydig:

```powershell
Copy-Item .env.example .env
notepad .env
.\scripts\start-local.ps1
```

Startskriptet vägrar starta med platshållare, korta eller återanvända secrets. Det validerar Compose, startar profilerna `core`, `apps`, `tools` och `observability` och kör därefter hela verifieringssviten.

Starta endast core-infrastruktur på en resurssnål utvecklingsmaskin:

```powershell
.\scripts\start-local.ps1 -CoreOnly
```

Manuell motsvarighet:

```powershell
docker compose --env-file .env --profile core --profile apps --profile tools --profile observability up -d
.\scripts\verify-local.ps1
```

## Tjänster och portar

Alla publicerade portar binds uttryckligen till loopback och kan ändras i `.env`.

| Komponent | Lokal adress | Profil | Användning |
|---|---|---|---|
| PostgreSQL | `127.0.0.1:5432` | `core` | Separata databaser och principals |
| Redis | `127.0.0.1:6379` | `core` | Autentiserad cache/rate-limit state |
| Kafka | `127.0.0.1:29092` | `core` | Extern listener för lokala appar |
| Keycloak | `http://127.0.0.1:8080` | `core` | OIDC och administration |
| Keycloak management | `http://127.0.0.1:9000` | `core` | Health och metrics |
| MailHog SMTP | `127.0.0.1:1025` | `core` | Lokala e-postutskick |
| MailHog UI | `http://127.0.0.1:8025` | `core` | Inspektera e-post |
| Kafka UI | `http://127.0.0.1:8081` | `tools` | Topics, partitions och messages |
| Product Service | `http://127.0.0.1:8082` | `apps` | Produkt-API, OpenAPI och Actuator |
| Prometheus | `http://127.0.0.1:9090` | `observability` | Metrics och queries |
| Grafana | `http://127.0.0.1:3000` | `observability` | Dashboards och Explore |
| Loki | `http://127.0.0.1:3100` | `observability` | Logg-API |
| Tempo | `http://127.0.0.1:3200` | `observability` | Trace query API |
| OTLP gRPC | `127.0.0.1:4317` | `observability` | Trace ingestion från host |
| OTLP HTTP | `127.0.0.1:4318` | `observability` | Trace ingestion från host |

Containrar använder adresserna `postgres:5432`, `redis:6379`, `kafka:9092`, `keycloak:8080`, `mailhog:1025`, `prometheus:9090`, `loki:3100` och `tempo:4317/4318` på Compose-nätverket.

## Databaser

En lokal PostgreSQL-instans innehåller nio isolerade databaser:

| Databas | Ägare |
|---|---|
| `gateway_db` | API Gateway |
| `identity_db` | Identity Service |
| `product_db` | Product Service |
| `inventory_db` | Inventory Service |
| `order_db` | Order Service |
| `payment_db` | Payment Service |
| `delivery_db` | Delivery Service |
| `notification_db` | Notification Service |
| `keycloak_db` | Keycloak |

Varje principal äger bara sin databas. `CONNECT` återkallas från `PUBLIC`, korsdatabasåtkomst verifieras negativt och monitoring-principalen får endast den inbyggda rollen `pg_monitor` mot bootstrapdatabasen. Init-skript körs endast när Postgres-volymen skapas första gången. Ändrade databaslösenord i `.env` påverkar därför inte en befintlig volym automatiskt.

## Kafka

Kafka kör en ensam kombinerad broker/controller i KRaft-läge. Automatisk topic creation är avstängd. `kafka-init` skapar fem domäntopics och fem DLTs från [eventkatalogen](../events/event-catalog.md), med explicita partitioner, retention och cleanup policy.

Containertrafik använder `kafka:9092`. Spring Boot-processer på värddatorn använder `localhost:29092`. Single-node-konfigurationen har replication factor och min ISR lika med ett och är därför endast avsedd för lokal utveckling.

## Keycloak

Realm `order-logistics` importeras automatiskt. Det innehåller:

- rollerna `CUSTOMER`, `ADMIN`, `SUPPORT` och `WAREHOUSE`;
- publika SPA-klienten `platform-web` med Authorization Code Flow och PKCE `S256`;
- bearer-only audience `platform-api`;
- en lokal verifieringsklient som ensam tillåter Direct Access Grant.

`keycloak-bootstrap` skapar idempotent följande lokala identiteter med lösenord från `.env`:

| Användare | Roll | Lösenordsvariabel |
|---|---|---|
| `customer@example.test` | `CUSTOMER` | `KEYCLOAK_CUSTOMER_PASSWORD` |
| `admin@example.test` | `ADMIN` | `KEYCLOAK_STAFF_PASSWORD` |
| `support@example.test` | `SUPPORT` | `KEYCLOAK_STAFF_PASSWORD` |
| `warehouse@example.test` | `WAREHOUSE` | `KEYCLOAK_STAFF_PASSWORD` |

Realmet använder `start-dev`, HTTP och en lokal verifieringsklient för reproducerbara tester. Ingen av dessa tre inställningar får flyttas till produktion.

Det publika issuer-värdet är `http://localhost:<KEYCLOAK_PORT>/realms/order-logistics`. Framtida tjänster som körs i Compose ska validera detta issuer-värde men använda den interna JWKS-adressen `http://keycloak:8080/realms/order-logistics/protocol/openid-connect/certs`; den explicita uppdelningen införs i Fas 8.

## Observability

- Prometheus skrapar plattformskomponenter och exporterare var 15:e sekund.
- Kafka Exporter exponerar consumer lag.
- Redis och PostgreSQL har separata exporterare.
- Grafana Alloy läser endast strukturerade JSON-loggar från den dedikerade named volume `platform-logs` och skickar dem till Loki; Docker-socketen exponeras inte.
- Tempo körs monolitiskt, tar emot OTLP/gRPC och OTLP/HTTP och lagrar traces på lokal filesystemvolym.
- Grafana provisionerar Prometheus, Loki och Tempo samt dashboarden `Order & Logistics Platform Overview`.

Dashboarden innehåller paneler för orderantal, misslyckade betalningar, Kafka consumer lag, API-svarstid, fel per tjänst, JVM heap och databasanslutningar. Applikationspanelerna visar noll eller tomt tills de relevanta Micrometer-mätetalen implementeras i Fas 3–8.

## Verifiering

```powershell
.\scripts\verify-local.ps1
```

Verifieringen kontrollerar:

- att corecontainrar körs och bootstrapjobb avslutats med exit code 0;
- inloggning till samtliga nio databaser med respektive ägare;
- att Product-principalen nekas anslutning till Order-databasen;
- samtliga topics och DLTs;
- autentiserad Redis `PING`;
- Keycloak readiness, OIDC discovery, audience och `CUSTOMER`-roll i testtoken;
- verklig SMTP-leverans till MailHog;
- valfria tools/observability-endpoints och Grafana-datasources.
- Product Service readiness och OpenAPI när `apps`-profilen körs.

## Stopp och återställning

Normalt stopp bevarar alla data:

```powershell
.\scripts\stop-local.ps1
```

Full återställning raderar permanent samtliga named volumes för det validerade Compose-projektet och kräver en exakt bekräftelsefras:

```powershell
.\scripts\stop-local.ps1 -RemoveVolumes
```

Använd `-Force` endast i automatiserad lokal testmiljö där dataförlusten redan är godkänd.

## Felsökning

```powershell
docker compose --env-file .env ps
docker compose --env-file .env logs postgres kafka keycloak
docker compose --env-file .env config
```

- Om initjobb misslyckas, läs jobbets logg med `docker compose logs kafka-init keycloak-bootstrap`.
- Om `.env` ändrats efter första Postgres-start, återställ volymen eller ändra principals kontrollerat; init-skriptet körs inte igen på en befintlig volym.
- Om Kafka annonserar fel endpoint, använd `localhost:<KAFKA_PORT>` från host och `kafka:9092` från containrar.
- Om en port är upptagen, ändra endast hostporten i `.env`.
- Om Keycloak realm-konfiguration ändras efter import, återställ den lokala Postgres-volymen för en ren reproducerbar import.

## Versionslåsning

Compose använder explicita versions-taggar och inga `latest`-taggar. Taggar gör builds reproducerbara på versionsnivå men är inte immutabla; CI/CD-fasen ska komplettera med digest-pinning, SBOM och Trivy-policy. Dependabot/Renovate-liknande uppdateringsautomation införs först tillsammans med CI.

## Officiella referenser

- [Apache Kafka Docker](https://kafka.apache.org/documentation/#docker)
- [Keycloak containers](https://www.keycloak.org/server/containers)
- [Keycloak realm import/export](https://www.keycloak.org/server/importExport)
- [Keycloak health checks](https://www.keycloak.org/observability/health)
- [Prometheus installation](https://prometheus.io/docs/prometheus/latest/installation/)
- [Grafana Docker installation](https://grafana.com/docs/grafana/latest/setup-grafana/installation/docker/)
- [Grafana Tempo local deployment](https://grafana.com/docs/tempo/latest/set-up-for-tracing/setup-tempo/deploy/locally/linux/)
- [Grafana Alloy in Docker](https://grafana.com/docs/alloy/latest/set-up/install/docker/)
- [Loki log ingestion with Alloy](https://grafana.com/docs/loki/latest/send-data/alloy/)

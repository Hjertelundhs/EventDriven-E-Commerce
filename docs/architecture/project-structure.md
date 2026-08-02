# Projektstruktur

## Efter Fas 2

```text
.
├── .env.example
├── .github/
│   └── workflows/
├── backend/
│   ├── api-gateway/
│   ├── identity-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── delivery-service/
│   ├── notification-service/
│   └── shared-contracts/
├── frontend/
├── infrastructure/
│   ├── docker/
│   │   └── postgres/
│   ├── kubernetes/
│   ├── helm/
│   ├── terraform/
│   ├── keycloak/
│   ├── monitoring/
│   └── messaging/
├── docs/
│   ├── architecture/
│   ├── adr/
│   ├── api/
│   └── events/
├── scripts/
│   ├── start-local.ps1
│   ├── stop-local.ps1
│   └── verify-local.ps1
├── docker-compose.yml
└── README.md
```

Tomma framtidskataloger innehåller `.gitkeep` så att strukturen versionshanteras utan att antyda att implementation finns. Fas 2 har fyllt katalogerna för Docker, Keycloak, messaging, monitoring och scripts med körbar lokal infrastruktur. Kubernetes, Helm och Terraform förblir avsiktligt tomma till Fas 10.

## Avsedd tjänstestruktur från respektive implementationsfas

```text
service-name/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/portfolio/<context>/
    │   │   ├── domain/
    │   │   ├── application/
    │   │   ├── api/
    │   │   ├── infrastructure/
    │   │   └── configuration/
    │   └── resources/
    │       ├── db/migration/
    │       └── application.yml
    └── test/
        └── java/com/portfolio/<context>/
```

Paketstrukturen är package-by-layer inom varje bounded context, men use cases och adapters grupperas vidare per feature när en tjänst växer. Beroenden pekar inåt: adapter → application → domain. Infrastructure implementerar application/domain-portar och får aldrig läcka JPA-entiteter in i domänen.

## Ägarskapsregler

- Varje tjänstkatalog har egen Maven-build, Flyway-historik, Dockerfile och testsvit.
- Det finns ingen delad domänmodul eller gemensam persistence-modul.
- `shared-contracts` innehåller JSON Schema, exempelpayloads och kompatibilitetstester, inte Java-domänklasser.
- Infrastrukturkonfiguration hålls utanför tjänsterna när den gäller plattformen; tjänstespecifik runtimekonfiguration ligger med tjänsten.
- Genererade filer, secrets och lokala volymer versionshanteras inte.

# Inventory Service

Phase 4 implementation for the inventory bounded context. The service is independently buildable, owns `inventory_db`, exposes its REST API and Actuator/OpenAPI on port `8083`, and is wired to `commerce.inventory.v1`.

## Included

- Java 21, Spring Boot 3.5 and Maven Wrapper.
- Framework-free `InventoryItem`, `InventoryReservation`, and `Sku` domain model.
- Stock arithmetic for receipt, adjustment, reservation, release and completion.
- Invariants preventing negative quantities and enforcing `total = available + reserved`.
- Hexagonal commands, queries, repository ports, and transactional application service.
- REST endpoints for stock lookup, receipt, adjustment, reservation, release, and completion.
- Stable reservation IDs, idempotent repeat requests, ETags, optimistic locking, and RFC 9457 errors.
- JPA adapters and Flyway schema for inventory items, reservations, processed events and transactional outbox.
- PostgreSQL, Kafka, OpenAPI, Prometheus, OTLP tracing and structured ECS logging configuration.
- Idempotent `OrderCreatedV1` consumption, atomic multi-line reservation and inventory result outbox publication.
- Non-root multi-stage Docker image and Compose `apps` profile wiring.
- Domain, application, controller, Compose contract, and PostgreSQL Testcontainers tests.

## Build and test

```powershell
cd backend/inventory-service
.\mvnw.cmd verify
```

On a host JDK newer than the JaCoCo release supports, run tests without instrumentation:

```powershell
.\mvnw.cmd -Djacoco.skip=true verify
```

## Local container

From the repository root:

```powershell
.\scripts\start-local.ps1
```

Readiness is exposed at `http://127.0.0.1:8083/actuator/health/readiness` and OpenAPI at `http://127.0.0.1:8083/v3/api-docs`.

## API

All mutating requests carry the expected current version in their JSON body. Responses expose the persisted version both in the body and as an `ETag`.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/inventory/{sku}` | Read available, reserved, and total stock |
| `POST` | `/api/v1/inventory/{sku}/receipts` | Receive physical stock |
| `PUT` | `/api/v1/inventory/{sku}/adjustment` | Set physical total stock |
| `POST` | `/api/v1/inventory/reservations` | Reserve stock for an order |
| `GET` | `/api/v1/inventory/reservations/{id}` | Read a reservation |
| `POST` | `/api/v1/inventory/reservations/{id}/release` | Return reserved stock to availability |
| `POST` | `/api/v1/inventory/reservations/{id}/completion` | Consume reserved stock |

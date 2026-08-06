# Order Service

Phase 5 implements the order bounded context and coordinates the asynchronous order saga. It owns `order_db`, publishes through a transactional outbox, consumes downstream events idempotently, and exposes customer-scoped REST and SSE APIs on port `8084`.

## Capabilities

- Strict aggregate state machine from `PENDING` through completion or compensation.
- Synchronous Product Service validation and immutable price/product snapshots.
- Customer-scoped idempotent registration using `X-Customer-ID` and `Idempotency-Key`.
- PostgreSQL, Flyway, optimistic locking, transactional outbox, and `processed_events` inbox.
- Kafka consumers for inventory, payment, and delivery events with retry and DLT recovery.
- `OrderCreatedV1`, `OrderCompletedV1`, and `OrderCancelledV1` events.
- RFC 9457 errors, correlation IDs, OpenAPI, Actuator, Prometheus, tracing, and structured logs.

`X-Customer-ID` is the explicit Phase 5 trust boundary. Phase 8 replaces it with the authenticated JWT subject without changing the application ports.

## API

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/orders` | Register an order (`202`, or `200` on idempotent replay) |
| `GET` | `/api/v1/orders/{id}` | Read an owned order |
| `GET` | `/api/v1/customers/me/orders` | List owned orders |
| `GET` | `/api/v1/orders/{id}/events` | Stream status changes with SSE |

## Verify

```powershell
cd backend/order-service
.\mvnw.cmd -Djacoco.skip=true verify
```

Run the local platform with `./scripts/start-local.ps1`. Readiness is at `http://127.0.0.1:8084/actuator/health/readiness` and OpenAPI at `http://127.0.0.1:8084/v3/api-docs`.

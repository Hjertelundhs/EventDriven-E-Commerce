# Payment Service — Phase 6

Payment Service owns the payment lifecycle and its audit trail. It consumes `OrderCreatedV1` to create a local pending payment, captures only after `InventoryReservedV1`, and starts an idempotent refund after `DeliveryFailedV1`.

The provider is deliberately simulated and configured with `PAYMENT_SIMULATION_MODE=SUCCESS|DECLINE|TIMEOUT`. Provider references are opaque generated identifiers. The service never accepts, stores, or logs PAN, CVV, cardholder, or other real card details.

## Run locally

```powershell
./mvnw.cmd verify
docker compose --env-file .env --profile core --profile apps up --detach --build payment-service
```

Read-only endpoints:

- `GET /api/v1/payments/{paymentId}`
- `GET /api/v1/payments/by-order/{orderId}`
- `GET /actuator/health/readiness`

Payment and refund commands are event-only to preserve the saga boundary.

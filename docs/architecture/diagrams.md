# Arkitekturdiagram

Diagrammen är källkod i Mermaid och ska uppdateras tillsammans med arkitekturen.

## System context

```mermaid
flowchart LR
    Customer["Kund"]
    Admin["Admin / Support / Lagerpersonal"]
    Platform["Order & Logistics Platform"]
    Keycloak["Keycloak\nIdentity Provider"]
    MailHog["MailHog\nSimulerad e-post"]

    Customer -->|"Handlar och följer order"| Platform
    Admin -->|"Administrerar behöriga resurser"| Platform
    Platform <-->|"OIDC / OAuth 2.0"| Keycloak
    Platform -->|"SMTP"| MailHog
```

## C4 container-diagram

```mermaid
C4Container
    title Container diagram — Order & Logistics Platform

    Person(customer, "Kund", "Handlar och följer sina orders")
    Person(staff, "Personal", "Admin, support eller lager")

    System_Ext(keycloak, "Keycloak", "OIDC identity provider")
    System_Ext(mailhog, "MailHog", "Lokal SMTP-mottagare")

    System_Boundary(platform, "Order & Logistics Platform") {
        Container(web, "Web App", "React, TypeScript, Vite", "Responsivt kund- och admingränssnitt")
        Container(gateway, "API Gateway", "Spring Cloud Gateway", "Routing, JWT, rate limiting och correlation")
        Container(identity, "Identity Service", "Spring Boot", "Profil och preferenser")
        Container(product, "Product Service", "Spring Boot", "Katalog och priser")
        Container(inventory, "Inventory Service", "Spring Boot", "Saldo och reservationer")
        Container(order, "Order Service", "Spring Boot", "Orderaggregate, state och SSE")
        Container(payment, "Payment Service", "Spring Boot", "Simulerad betalning och refund")
        Container(delivery, "Delivery Service", "Spring Boot", "Leverans och tracking")
        Container(notification, "Notification Service", "Spring Boot", "Asynkrona notifieringar")
        ContainerDb(postgres, "PostgreSQL databases", "PostgreSQL", "Separat databas och principal per tjänst")
        ContainerDb(redis, "Redis", "Redis", "Cache och rate-limit state")
        ContainerQueue(kafka, "Kafka", "Apache Kafka", "Versionshanterade domänhändelser")
        Container(observability, "Observability", "Prometheus, Grafana, Loki, Tempo", "Metrics, logs och traces")
    }

    Rel(customer, web, "Använder", "HTTPS")
    Rel(staff, web, "Använder", "HTTPS")
    Rel(web, keycloak, "Loggar in", "OIDC + PKCE")
    Rel(web, gateway, "Anropar API / prenumererar", "HTTPS / SSE")
    Rel(gateway, identity, "Routar", "REST")
    Rel(gateway, product, "Routar", "REST")
    Rel(gateway, inventory, "Routar", "REST")
    Rel(gateway, order, "Routar", "REST / SSE")
    Rel(gateway, delivery, "Routar", "REST")
    Rel(order, product, "Validerar produkt och pris", "REST")
    Rel(order, kafka, "Publicerar/konsumerar", "Kafka")
    Rel(inventory, kafka, "Publicerar/konsumerar", "Kafka")
    Rel(payment, kafka, "Publicerar/konsumerar", "Kafka")
    Rel(delivery, kafka, "Publicerar/konsumerar", "Kafka")
    Rel(notification, kafka, "Konsumerar", "Kafka")
    Rel(notification, mailhog, "Skickar simulerad e-post", "SMTP")
    Rel(gateway, redis, "Rate limiting", "RESP/TLS")
    Rel(product, redis, "Produktcache", "RESP/TLS")
    Rel(gateway, postgres, "Äger gateway-databas", "JDBC/R2DBC")
    Rel(identity, postgres, "Äger identity-databas", "JDBC")
    Rel(product, postgres, "Äger product-databas", "JDBC")
    Rel(inventory, postgres, "Äger inventory-databas", "JDBC")
    Rel(order, postgres, "Äger order-databas", "JDBC")
    Rel(payment, postgres, "Äger payment-databas", "JDBC")
    Rel(delivery, postgres, "Äger delivery-databas", "JDBC")
    Rel(notification, postgres, "Äger notification-databas", "JDBC")
    Rel(gateway, observability, "Telemetri", "OTLP / scrape / logs")
```

`PostgreSQL databases` visas som en container för läsbarhet men representerar åtta logiskt separata databaser med separata principals och inget korsägande.

## Tjänster och asynkrona fakta

```mermaid
flowchart LR
    OS["Order Service"]
    IS["Inventory Service"]
    PS["Payment Service"]
    DS["Delivery Service"]
    NS["Notification Service"]
    K[("Kafka")]

    OS -->|"OrderCreated / OrderCompleted / OrderCancelled"| K
    K -->|"OrderCreated"| IS
    K -->|"OrderCreated — betalningsunderlag"| PS
    K -->|"OrderCreated — fulfillmentunderlag"| DS
    IS -->|"InventoryReserved / ReservationFailed / Released"| K
    K -->|"Inventory events"| OS
    K -->|"InventoryReserved"| PS
    PS -->|"PaymentCompleted / PaymentFailed / Refund events"| K
    K -->|"Payment events"| OS
    K -->|"PaymentCompleted"| DS
    DS -->|"DeliveryCreated / DeliveryFailed / StatusChanged"| K
    K -->|"Delivery events"| OS
    K -->|"DeliveryFailed"| PS
    K -->|"PaymentFailed / DeliveryFailed / OrderCompleted"| IS
    K -->|"Notifierbara domänhändelser"| NS
```

## Saga-sekvens — lyckat flöde

```mermaid
sequenceDiagram
    autonumber
    actor C as Kund
    participant G as API Gateway
    participant O as Order Service
    participant P as Product Service
    participant K as Kafka
    participant I as Inventory Service
    participant Pay as Payment Service
    participant D as Delivery Service
    participant N as Notification Service

    C->>G: POST /api/v1/orders (Idempotency-Key)
    G->>O: Verifierad JWT + request
    O->>P: Validera SKU och aktuell pris-snapshot
    P-->>O: Aktiva produkter och priser
    O->>O: Spara PENDING + outbox atomiskt
    O-->>G: 202 Accepted + orderId
    G-->>C: Status- och SSE-länkar
    O->>K: OrderCreatedV1
    K->>I: OrderCreatedV1
    K->>Pay: OrderCreatedV1 (lagra betalningsunderlag)
    K->>D: OrderCreatedV1 (lagra fulfillmentunderlag)
    I->>I: Reservera + outbox atomiskt
    I->>K: InventoryReservedV1
    K->>O: InventoryReservedV1
    K->>Pay: InventoryReservedV1
    Pay->>Pay: Capture idempotent + outbox
    Pay->>K: PaymentCompletedV1
    K->>O: PaymentCompletedV1
    K->>D: PaymentCompletedV1
    D->>D: Skapa unik leverans + outbox
    D->>K: DeliveryCreatedV1
    K->>O: DeliveryCreatedV1
    D->>K: DeliveryStatusChangedV1 (DELIVERED)
    K->>O: DeliveryStatusChangedV1
    O->>K: OrderCompletedV1
    K->>I: OrderCompletedV1
    K->>N: Notifierbara events
    O-->>C: SSE-statusuppdateringar
```

## Saga-sekvens — kompensationer

```mermaid
sequenceDiagram
    autonumber
    participant O as Order Service
    participant K as Kafka
    participant I as Inventory Service
    participant Pay as Payment Service
    participant D as Delivery Service

    alt Lager kan inte reserveras
        I->>K: InventoryReservationFailedV1
        K->>O: Avbryt order
        O->>K: OrderCancelledV1
    else Betalning misslyckas
        Pay->>K: PaymentFailedV1
        K->>O: Markera PAYMENT_FAILED
        K->>I: Släpp reservation idempotent
        I->>K: InventoryReleasedV1
        K->>O: Markera CANCELLED
        O->>K: OrderCancelledV1
    else Leverans kan inte skapas
        D->>K: DeliveryFailedV1
        K->>O: Markera DELIVERY_FAILED
        K->>Pay: Markera REFUND_PENDING
        Pay->>K: RefundRequestedV1
        Pay->>K: RefundCompletedV1
        K->>I: Släpp/återför reservation
        I->>K: InventoryReleasedV1
        K->>O: Registrera båda kompensationer
        O->>K: OrderCancelledV1
    end
```

## Order state machine

```mermaid
stateDiagram-v2
    [*] --> PENDING: Order accepterad
    PENDING --> INVENTORY_RESERVED: InventoryReservedV1
    INVENTORY_RESERVED --> PAYMENT_PENDING: Betalning initierad
    PAYMENT_PENDING --> PAID: PaymentCompletedV1
    PAID --> DELIVERY_CREATED: DeliveryCreatedV1
    DELIVERY_CREATED --> COMPLETED: DeliveryStatus = DELIVERED
    PENDING --> CANCELLED: InventoryReservationFailedV1
    PAYMENT_PENDING --> PAYMENT_FAILED: PaymentFailedV1
    PAYMENT_FAILED --> CANCELLED: InventoryReleasedV1
    PAID --> DELIVERY_FAILED: DeliveryFailedV1
    DELIVERY_FAILED --> CANCELLED: Refund + inventory compensation klara
    COMPLETED --> [*]
    CANCELLED --> [*]
```

## Transactional outbox och idempotent consumer

```mermaid
sequenceDiagram
    participant U as Use case
    participant DB as Service database
    participant OP as Outbox publisher
    participant K as Kafka
    participant C as Consumer
    participant CDB as Consumer database

    U->>DB: BEGIN
    U->>DB: Ändra aggregate
    U->>DB: INSERT outbox_events
    U->>DB: COMMIT
    OP->>DB: Hämta opublicerad batch med lås
    OP->>K: Publicera med stabilt eventId
    K-->>OP: Ack
    OP->>DB: Markera publicerad
    K->>C: Leverera event (at-least-once)
    C->>CDB: BEGIN + INSERT processed_events
    C->>CDB: Tillämpa affärseffekt
    C->>CDB: COMMIT
    Note over C,CDB: Unik eventId gör omleverans till no-op
```

## Databasägarskap

```mermaid
flowchart TB
    G["API Gateway"] --> GDB[("gateway_db")]
    ID["Identity Service"] --> IDDB[("identity_db")]
    P["Product Service"] --> PDB[("product_db")]
    I["Inventory Service"] --> IDATA[("inventory_db")]
    O["Order Service"] --> ODB[("order_db")]
    Pay["Payment Service"] --> PayDB[("payment_db")]
    D["Delivery Service"] --> DDB[("delivery_db")]
    N["Notification Service"] --> NDB[("notification_db")]

    classDef db fill:#e8f4fd,stroke:#1d70b8,color:#111;
    class GDB,IDDB,PDB,IDATA,ODB,PayDB,DDB,NDB db;
```

Det finns avsiktligt inga pilar mellan en tjänst och en annan tjänsts databas.

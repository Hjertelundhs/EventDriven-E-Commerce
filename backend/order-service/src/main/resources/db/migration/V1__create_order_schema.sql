CREATE TABLE orders (
    id UUID PRIMARY KEY, customer_id UUID NOT NULL, status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL CHECK (total_amount >= 0), currency VARCHAR(3) NOT NULL,
    shipping_recipient VARCHAR(200) NOT NULL, shipping_line1 VARCHAR(255) NOT NULL, shipping_line2 VARCHAR(255), shipping_postal_code VARCHAR(32) NOT NULL, shipping_city VARCHAR(120) NOT NULL, shipping_country_code VARCHAR(2) NOT NULL,
    billing_recipient VARCHAR(200) NOT NULL, billing_line1 VARCHAR(255) NOT NULL, billing_line2 VARCHAR(255), billing_postal_code VARCHAR(32) NOT NULL, billing_city VARCHAR(120) NOT NULL, billing_country_code VARCHAR(2) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL, request_fingerprint VARCHAR(64) NOT NULL,
    inventory_released BOOLEAN NOT NULL DEFAULT FALSE, refund_completed BOOLEAN NOT NULL DEFAULT FALSE, cancellation_reason VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_orders_customer_idempotency UNIQUE(customer_id,idempotency_key)
);
CREATE INDEX idx_orders_customer_created ON orders(customer_id,created_at DESC);
CREATE TABLE order_lines (
    id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE, product_id UUID NOT NULL,
    sku VARCHAR(80) NOT NULL, product_name VARCHAR(200) NOT NULL, quantity INTEGER NOT NULL CHECK(quantity > 0),
    unit_price NUMERIC(19,2) NOT NULL CHECK(unit_price >= 0), total_price NUMERIC(19,2) NOT NULL CHECK(total_price >= 0)
);
CREATE INDEX idx_order_lines_order ON order_lines(order_id);
CREATE TABLE processed_events (
    consumer_group VARCHAR(100) NOT NULL, event_id UUID NOT NULL, event_type VARCHAR(120) NOT NULL, processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(consumer_group,event_id)
);
CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY, aggregate_id UUID NOT NULL, event_type VARCHAR(120) NOT NULL, event_version INTEGER NOT NULL,
    correlation_id UUID NOT NULL, causation_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, next_attempt_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ, last_error VARCHAR(500), created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_order_outbox_pending ON outbox_events(status,next_attempt_at,created_at);

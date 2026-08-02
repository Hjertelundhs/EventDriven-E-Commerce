CREATE TABLE products (
    id UUID PRIMARY KEY,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    category VARCHAR(120) NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_products_currency_uppercase CHECK (currency = upper(currency)),
    CONSTRAINT ck_products_timestamps CHECK (updated_at >= created_at),
    CONSTRAINT ck_products_version_non_negative CHECK (version >= 0)
);

CREATE INDEX idx_products_active_name ON products (active, lower(name));
CREATE INDEX idx_products_category_active ON products (lower(category), active);
CREATE INDEX idx_products_created_at ON products (created_at DESC);

CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    event_version INTEGER NOT NULL,
    correlation_id UUID NOT NULL,
    causation_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_outbox_event_version_positive CHECK (event_version > 0),
    CONSTRAINT ck_outbox_attempts_non_negative CHECK (attempts >= 0),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED')),
    CONSTRAINT ck_outbox_payload_object CHECK (jsonb_typeof(payload) = 'object')
);

CREATE INDEX idx_outbox_pending
    ON outbox_events (next_attempt_at, created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_outbox_aggregate ON outbox_events (aggregate_id, occurred_at);

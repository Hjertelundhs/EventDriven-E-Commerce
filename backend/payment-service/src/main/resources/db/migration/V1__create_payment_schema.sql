CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    amount NUMERIC(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(24) NOT NULL,
    provider_reference VARCHAR(120),
    failure_reason VARCHAR(64),
    refund_id UUID,
    refund_provider_reference VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_payment_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT ck_payment_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_payment_status CHECK (status IN ('PENDING','COMPLETED','FAILED','REFUND_PENDING','REFUNDED','REFUND_FAILED')),
    CONSTRAINT ck_payment_timestamps CHECK (updated_at >= created_at)
);
CREATE INDEX idx_payments_status ON payments(status);

CREATE TABLE payment_audit (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id),
    action VARCHAR(64) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    safe_detail VARCHAR(160),
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_payment_audit_payment ON payment_audit(payment_id, occurred_at);

CREATE TABLE processed_events (
    consumer_group VARCHAR(120) NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer_group, event_id)
);

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
    CONSTRAINT ck_payment_outbox_status CHECK (status IN ('PENDING','PUBLISHED')),
    CONSTRAINT ck_payment_outbox_payload CHECK (jsonb_typeof(payload) = 'object')
);
CREATE INDEX idx_payment_outbox_pending ON outbox_events(next_attempt_at, created_at) WHERE status = 'PENDING';

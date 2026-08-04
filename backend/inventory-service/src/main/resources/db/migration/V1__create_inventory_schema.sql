CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    sku VARCHAR(64) NOT NULL,
    available_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    total_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_inventory_items_sku UNIQUE (sku),
    CONSTRAINT ck_inventory_available_non_negative CHECK (available_quantity >= 0),
    CONSTRAINT ck_inventory_reserved_non_negative CHECK (reserved_quantity >= 0),
    CONSTRAINT ck_inventory_total_non_negative CHECK (total_quantity >= 0),
    CONSTRAINT ck_inventory_quantity_balance CHECK (total_quantity = available_quantity + reserved_quantity),
    CONSTRAINT ck_inventory_timestamps CHECK (updated_at >= created_at),
    CONSTRAINT ck_inventory_version_non_negative CHECK (version >= 0)
);

CREATE INDEX idx_inventory_items_sku ON inventory_items (sku);
CREATE INDEX idx_inventory_items_available ON inventory_items (available_quantity) WHERE available_quantity > 0;

CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    inventory_item_id UUID NOT NULL,
    order_id UUID NOT NULL,
    sku VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inventory_reservation_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id),
    CONSTRAINT uk_inventory_reservation_order_sku UNIQUE (order_id, sku),
    CONSTRAINT ck_inventory_reservation_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_inventory_reservation_status CHECK (status IN ('RESERVED', 'RELEASED', 'COMPLETED')),
    CONSTRAINT ck_inventory_reservation_timestamps CHECK (updated_at >= created_at),
    CONSTRAINT ck_inventory_reservation_version_non_negative CHECK (version >= 0)
);

CREATE INDEX idx_inventory_reservations_order ON inventory_reservations (order_id);
CREATE INDEX idx_inventory_reservations_active ON inventory_reservations (inventory_item_id, status) WHERE status = 'RESERVED';

CREATE TABLE processed_events (
    consumer_group VARCHAR(120) NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer_group, event_id)
);

CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);

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
    CONSTRAINT ck_inventory_outbox_event_version_positive CHECK (event_version > 0),
    CONSTRAINT ck_inventory_outbox_attempts_non_negative CHECK (attempts >= 0),
    CONSTRAINT ck_inventory_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED')),
    CONSTRAINT ck_inventory_outbox_payload_object CHECK (jsonb_typeof(payload) = 'object')
);

CREATE INDEX idx_inventory_outbox_pending
    ON outbox_events (next_attempt_at, created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_inventory_outbox_aggregate ON outbox_events (aggregate_id, occurred_at);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(80) NOT NULL UNIQUE,
    request_fingerprint VARCHAR(64) NOT NULL,
    merchant_reference VARCHAR(64) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    debtor_account_token VARCHAR(64) NOT NULL,
    creditor_account_token VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    status_reason VARCHAR(160),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE payment_transitions (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id),
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(160),
    sequence BIGINT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(payment_id, sequence)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL REFERENCES payments(id),
    event_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payments_status_created ON payments(status, created_at);
CREATE INDEX idx_payment_transitions_payment ON payment_transitions(payment_id, sequence);
CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);

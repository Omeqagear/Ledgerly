-- V3__create_payments.sql
CREATE TABLE payments (
    id                    UUID PRIMARY KEY,
    invoice_id            UUID          NOT NULL,
    customer_id           UUID          NOT NULL,
    amount                NUMERIC(19,2) NOT NULL,
    payment_method        VARCHAR(32)   NOT NULL,
    transaction_reference VARCHAR(128),
    status                VARCHAR(16)   NOT NULL,
    failure_reason        VARCHAR(512),
    processed_at          TIMESTAMP
);

CREATE INDEX ix_payments_invoice  ON payments (invoice_id);
CREATE INDEX ix_payments_customer ON payments (customer_id);
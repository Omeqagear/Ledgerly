-- V2__create_invoices.sql
CREATE TABLE invoices (
    id                   UUID PRIMARY KEY,
    customer_id          UUID         NOT NULL,
    invoice_number       VARCHAR(32)  NOT NULL,
    total_amount         NUMERIC(19,2) NOT NULL,
    tax_amount           NUMERIC(19,2) NOT NULL,
    issue_date           DATE         NOT NULL,
    due_date            DATE         NOT NULL,
    status               VARCHAR(16)  NOT NULL,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP
);

CREATE UNIQUE INDEX ux_invoices_number ON invoices (invoice_number);
CREATE INDEX ix_invoices_customer     ON invoices (customer_id);
CREATE INDEX ix_invoices_status_due   ON invoices (status, due_date);
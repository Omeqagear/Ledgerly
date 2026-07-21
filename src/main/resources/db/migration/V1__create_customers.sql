-- V1__create_customers.sql
CREATE TABLE customers (
    id                   UUID PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL,
    email                VARCHAR(255) NOT NULL,
    tax_id               VARCHAR(64),
    address              VARCHAR(512),
    preferred_language   VARCHAR(8)  NOT NULL DEFAULT 'en',
    created_at           TIMESTAMP   NOT NULL
);

CREATE UNIQUE INDEX ux_customers_email ON customers (email);
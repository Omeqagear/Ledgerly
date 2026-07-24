-- V6__create_users.sql
CREATE TABLE users (
    id               UUID PRIMARY KEY,
    username         VARCHAR(64) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    role             VARCHAR(16) NOT NULL,
    created_at       TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX ux_users_username ON users (username);
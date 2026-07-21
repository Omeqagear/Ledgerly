-- V4__create_event_publication.sql
-- Spring Modulith persistent domain-event publication registry.
-- DDL mirrors what Hibernate generates for the Modulith JPA event-publication
-- entity (so ddl-auto=validate passes). The two date columns are timestamptz
-- because the entity maps them as java.time.Instant.
CREATE TABLE event_publication (
    completion_date  TIMESTAMP(6) WITH TIME ZONE,
    publication_date TIMESTAMP(6) WITH TIME ZONE,
    id               UUID         NOT NULL PRIMARY KEY,
    event_type       VARCHAR(255),
    listener_id      VARCHAR(255),
    serialized_event VARCHAR(255)
);
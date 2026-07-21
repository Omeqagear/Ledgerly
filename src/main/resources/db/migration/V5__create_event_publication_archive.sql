-- V5__create_event_publication_archive.sql
-- Spring Modulith event-publication archive (used when
-- spring.modulith.events.completion-mode=ARCHIVE). The table is mapped by the
-- Modulith JPA starter regardless of the configured completion mode, so it
-- must exist for ddl-auto=validate even though we run with DELETE.
CREATE TABLE event_publication_archive (
    completion_date  TIMESTAMP(6) WITH TIME ZONE,
    publication_date TIMESTAMP(6) WITH TIME ZONE,
    id               UUID         NOT NULL PRIMARY KEY,
    event_type       VARCHAR(255),
    listener_id      VARCHAR(255),
    serialized_event VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS sequence_registry (
    sequence_type VARCHAR(64) PRIMARY KEY,
    current_value BIGINT      NOT NULL,
    increment_by  BIGINT      NOT NULL DEFAULT 1
);

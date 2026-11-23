CREATE TABLE outbox (
    id           UUID                        NOT NULL PRIMARY KEY,
    topic        VARCHAR(255)                NOT NULL,
    message_key  VARCHAR(255)                NOT NULL,
    payload      TEXT                        NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP(6) WITH TIME ZONE
);

-- O relay só lê pendentes; índice parcial fica pequeno mesmo com o histórico crescendo
CREATE INDEX idx_outbox_pending ON outbox (created_at) WHERE published_at IS NULL;

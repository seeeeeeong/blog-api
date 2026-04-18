CREATE TABLE IF NOT EXISTS post_outbox (
    id                BIGSERIAL PRIMARY KEY,
    event_id          UUID        NOT NULL UNIQUE,
    post_id           BIGINT      NOT NULL,
    event_type        VARCHAR(20) NOT NULL,
    payload           TEXT        NOT NULL,
    attempts          INT         NOT NULL DEFAULT 0,
    last_error        TEXT,
    next_attempt_at   TIMESTAMP   NOT NULL DEFAULT now(),
    processed_at      TIMESTAMP,
    created_at        TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_post_outbox_pending
    ON post_outbox (next_attempt_at)
    WHERE processed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_post_outbox_post_id
    ON post_outbox (post_id);

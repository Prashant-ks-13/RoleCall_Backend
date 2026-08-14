CREATE TABLE payment_transactions (
    id                          BINARY(16)     NOT NULL,
    user_id                     BINARY(16)     NOT NULL,
    job_id                      BINARY(16)     NULL,
    type                        VARCHAR(20)    NOT NULL,
    amount                      DECIMAL(10,2)  NOT NULL,
    currency                    VARCHAR(3)     NOT NULL,
    stripe_session_id           VARCHAR(255)   NULL,
    stripe_payment_intent_id    VARCHAR(255)   NULL,
    status                      VARCHAR(20)    NOT NULL,
    created_at                  TIMESTAMP(6)   NOT NULL,
    updated_at                  TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_transactions_stripe_session_id (stripe_session_id),
    KEY idx_payment_transactions_user_id (user_id),
    KEY idx_payment_transactions_job_id (job_id)
);

CREATE TABLE webhook_events (
    id                BINARY(16)     NOT NULL,
    stripe_event_id   VARCHAR(255)   NOT NULL,
    type              VARCHAR(100)   NOT NULL,
    received_at       TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_webhook_events_stripe_event_id (stripe_event_id)
);

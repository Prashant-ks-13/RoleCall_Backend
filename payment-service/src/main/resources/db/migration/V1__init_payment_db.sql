CREATE TABLE payment_transactions (
    id                          BINARY(16)     NOT NULL,
    user_id                     BINARY(16)     NOT NULL,
    job_id                      BINARY(16)     NULL,
    type                        VARCHAR(20)    NOT NULL,
    amount                      DECIMAL(10,2)  NOT NULL,
    currency                    VARCHAR(3)     NOT NULL,
    razorpay_payment_link_id    VARCHAR(255)   NULL,
    razorpay_payment_id         VARCHAR(255)   NULL,
    status                      VARCHAR(20)    NOT NULL,
    created_at                  TIMESTAMP(6)   NOT NULL,
    updated_at                  TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_transactions_razorpay_payment_link_id (razorpay_payment_link_id),
    KEY idx_payment_transactions_user_id (user_id),
    KEY idx_payment_transactions_job_id (job_id)
);

CREATE TABLE webhook_events (
    id                  BINARY(16)     NOT NULL,
    razorpay_event_id   VARCHAR(255)   NOT NULL,
    type                VARCHAR(100)   NOT NULL,
    received_at         TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_webhook_events_razorpay_event_id (razorpay_event_id)
);

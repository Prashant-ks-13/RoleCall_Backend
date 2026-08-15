CREATE TABLE users (
    id              BINARY(16)      NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP(6)    NOT NULL,
    updated_at      TIMESTAMP(6)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE refresh_tokens (
    id                    BINARY(16)    NOT NULL,
    user_id               BINARY(16)    NOT NULL,
    token_hash            VARCHAR(64)   NOT NULL,
    expires_at            TIMESTAMP(6)  NOT NULL,
    revoked               BOOLEAN       NOT NULL DEFAULT FALSE,
    replaced_by_token_id  BINARY(16)    NULL,
    created_at            TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token_hash (token_hash),
    KEY idx_refresh_tokens_user_id (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

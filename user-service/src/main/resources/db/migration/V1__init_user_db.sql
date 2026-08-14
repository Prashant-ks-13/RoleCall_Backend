CREATE TABLE user_profiles (
    id                BINARY(16)     NOT NULL,
    email             VARCHAR(255)   NOT NULL,
    role              VARCHAR(20)    NOT NULL,
    full_name         VARCHAR(150)   NULL,
    headline          VARCHAR(150)   NULL,
    bio               VARCHAR(2000)  NULL,
    phone             VARCHAR(30)    NULL,
    location          VARCHAR(150)   NULL,
    resume_url        VARCHAR(500)   NULL,
    avatar_url        VARCHAR(500)   NULL,
    company_name      VARCHAR(150)   NULL,
    company_website   VARCHAR(300)   NULL,
    created_at        TIMESTAMP(6)   NOT NULL,
    updated_at        TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_profiles_email (email)
);

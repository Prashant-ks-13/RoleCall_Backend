CREATE TABLE job_applications (
    id                   BINARY(16)    NOT NULL,
    job_id               BINARY(16)    NOT NULL,
    candidate_id         BINARY(16)    NOT NULL,
    status               VARCHAR(20)   NOT NULL,
    resume_snapshot_url  VARCHAR(500)  NULL,
    created_at           TIMESTAMP(6)  NOT NULL,
    updated_at           TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_applications_job_candidate (job_id, candidate_id),
    KEY idx_job_applications_candidate_id (candidate_id),
    KEY idx_job_applications_job_id (job_id)
);

CREATE TABLE application_status_history (
    id              BINARY(16)     NOT NULL,
    application_id  BINARY(16)     NOT NULL,
    from_status     VARCHAR(20)    NULL,
    to_status       VARCHAR(20)    NOT NULL,
    changed_by      BINARY(16)     NOT NULL,
    changed_at      TIMESTAMP(6)   NOT NULL,
    note            VARCHAR(1000)  NULL,
    PRIMARY KEY (id),
    KEY idx_application_status_history_application_id (application_id),
    CONSTRAINT fk_status_history_application FOREIGN KEY (application_id) REFERENCES job_applications (id)
);

ALTER TABLE certifications
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE AFTER status,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER reviewed_at,
    ADD INDEX idx_certification_availability (deleted_at, enabled, category, status);

ALTER TABLE jobs
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER active;

ALTER TABLE discovery_categories
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER active;

ALTER TABLE discovery_matters
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER active;

ALTER TABLE discovery_experiences
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER active;

ALTER TABLE user_jobs
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER verified;

ALTER TABLE discovery_matter_jobs
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE AFTER sort_order,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER active;

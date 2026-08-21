ALTER TABLE certifications
    ADD COLUMN authenticity_percent INT NULL AFTER years,
    ADD COLUMN job_reapply_available_at DATETIME(6) NULL AFTER authenticity_percent,
    ADD CONSTRAINT chk_certification_authenticity_percent
        CHECK (
            authenticity_percent IS NULL
            OR authenticity_percent IN (0, 20, 40, 51, 60, 80, 90, 100)
        );

ALTER TABLE users
    ADD COLUMN job_certification_blocked_until DATETIME(6) NULL AFTER answerer_status,
    ADD INDEX idx_user_job_certification_blocked_until
        (job_certification_blocked_until);

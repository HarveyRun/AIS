ALTER TABLE certifications
    ADD COLUMN privacy_confirmed_at DATETIME NULL AFTER reviewed_at,
    ADD COLUMN submitter_signature VARCHAR(50) NULL AFTER privacy_confirmed_at;

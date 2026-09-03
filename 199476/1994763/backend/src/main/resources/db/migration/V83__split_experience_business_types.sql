ALTER TABLE certifications
    ADD COLUMN experience_business_type VARCHAR(24) NULL AFTER certification_type,
    ADD COLUMN monetization_amount DECIMAL(12, 2) NULL AFTER experience_business_type,
    ADD COLUMN upgrade_source_id BIGINT NULL AFTER monetization_amount;

UPDATE certifications c
LEFT JOIN users u ON u.id = c.user_id
SET c.experience_business_type = 'MONETIZED',
    c.monetization_amount = LEAST(
        5000,
        GREATEST(1, COALESCE(NULLIF(u.inquiry_price_min, 0), 1))
    )
WHERE c.category = 'EXPERIENCE';

CREATE INDEX idx_certifications_experience_business
    ON certifications(category, experience_business_type, status, enabled, deleted_at);

CREATE INDEX idx_certifications_upgrade_source
    ON certifications(upgrade_source_id, deleted_at);

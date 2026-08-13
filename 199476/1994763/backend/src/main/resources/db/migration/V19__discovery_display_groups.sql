ALTER TABLE discovery_matters
    ADD COLUMN display_group VARCHAR(20) NOT NULL DEFAULT 'COMMON' AFTER title,
    ADD CONSTRAINT chk_discovery_matter_display_group
        CHECK (display_group IN ('COMMON', 'MORE')),
    ADD INDEX idx_discovery_matter_group (display_group, active, category_id, sort_order, id);

ALTER TABLE certifications
    ADD COLUMN discovery_display_group VARCHAR(20) NULL AFTER discovery_category_id,
    ADD CONSTRAINT chk_certification_discovery_display_group
        CHECK (discovery_display_group IS NULL OR discovery_display_group IN ('COMMON', 'MORE')),
    ADD INDEX idx_certification_discovery_group
        (category, status, discovery_display_group, discovery_category_id);

UPDATE certifications
SET discovery_display_group = 'COMMON'
WHERE category = 'EXPERIENCE' AND status = 'APPROVED';

ALTER TABLE certifications
    DROP CHECK chk_certification_discovery_display_group,
    DROP INDEX idx_certification_discovery_group,
    DROP COLUMN discovery_display_group;

ALTER TABLE certifications
    DROP CHECK chk_certification_discovery_display_group,
    ADD CONSTRAINT chk_certification_discovery_display_group
        CHECK (discovery_display_group IS NULL OR discovery_display_group IN ('COMMON', 'MORE', 'HIDDEN'));

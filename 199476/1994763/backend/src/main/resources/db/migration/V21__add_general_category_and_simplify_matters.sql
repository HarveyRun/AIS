ALTER TABLE discovery_matters
    DROP CHECK chk_discovery_matter_display_group,
    DROP INDEX idx_discovery_matter_group,
    DROP COLUMN display_group;

INSERT INTO discovery_categories(main_category, name, sort_order, active)
VALUES
    ('GENERAL', '常见事项', 1, TRUE),
    ('GENERAL', '更多事项', 2, TRUE);

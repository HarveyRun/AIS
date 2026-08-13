ALTER TABLE discovery_categories
    ADD COLUMN content_scope VARCHAR(20) NOT NULL DEFAULT 'BOTH' AFTER name,
    ADD CONSTRAINT chk_discovery_category_content_scope
        CHECK (content_scope IN ('BOTH', 'MATTERS', 'EXPERIENCES'));

UPDATE discovery_categories
SET content_scope = 'MATTERS'
WHERE main_category = 'GENERAL'
  AND name IN ('常见事项', '更多事项');

INSERT INTO discovery_categories(main_category, name, content_scope, sort_order, active)
VALUES
    ('GENERAL', '常见经历', 'EXPERIENCES', 3, TRUE),
    ('GENERAL', '更多经历', 'EXPERIENCES', 4, TRUE);

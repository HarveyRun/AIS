UPDATE certifications
SET required_item = FALSE
WHERE certification_type = 'MAIN_JOB'
  AND deleted_at IS NULL;

UPDATE users u
SET u.answerer_status = CASE
        WHEN EXISTS (
            SELECT 1
            FROM certifications ci
            WHERE ci.user_id = u.id
              AND ci.certification_type = 'IDENTITY'
              AND ci.status = 'APPROVED'
              AND ci.enabled = TRUE
              AND ci.deleted_at IS NULL
        )
        AND EXISTS (
            SELECT 1
            FROM certifications ce
            WHERE ce.user_id = u.id
              AND ce.category = 'EXPERIENCE'
              AND ce.status = 'APPROVED'
              AND ce.enabled = TRUE
              AND ce.deleted_at IS NULL
        ) THEN 'APPROVED'
        ELSE 'PENDING'
    END,
    u.accepting_inquiries = CASE
        WHEN EXISTS (
            SELECT 1
            FROM certifications ci
            WHERE ci.user_id = u.id
              AND ci.certification_type = 'IDENTITY'
              AND ci.status = 'APPROVED'
              AND ci.enabled = TRUE
              AND ci.deleted_at IS NULL
        )
        AND EXISTS (
            SELECT 1
            FROM certifications ce
            WHERE ce.user_id = u.id
              AND ce.category = 'EXPERIENCE'
              AND ce.status = 'APPROVED'
              AND ce.enabled = TRUE
              AND ce.deleted_at IS NULL
        ) THEN u.accepting_inquiries
        ELSE FALSE
    END
WHERE u.answerer_status <> 'CLOSED';

UPDATE analytics_event_definitions
SET display_name = '搜索亲身经历'
WHERE event_name = 'home_search_submit';

ALTER TABLE inquiries
    ADD COLUMN source_experience_certification_id BIGINT NULL AFTER source_type,
    ADD INDEX idx_inquiry_source_experience (source_experience_certification_id),
    ADD CONSTRAINT fk_inquiry_source_experience
        FOREIGN KEY (source_experience_certification_id) REFERENCES certifications(id);

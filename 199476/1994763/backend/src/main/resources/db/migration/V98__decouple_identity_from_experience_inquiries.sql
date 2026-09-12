UPDATE users u
SET u.answerer_status = CASE
        WHEN EXISTS (
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

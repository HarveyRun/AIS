ALTER TABLE certifications
    ADD COLUMN legacy_experience_business_type VARCHAR(24) NULL AFTER experience_business_type,
    ADD COLUMN legacy_experience_status VARCHAR(24) NULL AFTER legacy_experience_business_type;

UPDATE certifications
SET legacy_experience_business_type = experience_business_type,
    legacy_experience_status = status
WHERE category = 'EXPERIENCE';

UPDATE certifications
SET status = 'REJECTED',
    rejection_reason = '发布流程已统一，请按当前要求补充后重新提交',
    reviewed_at = CURRENT_TIMESTAMP(6)
WHERE category = 'EXPERIENCE'
  AND legacy_experience_business_type = 'PUBLIC_WELFARE'
  AND status IN ('PENDING', 'APPROVED');

UPDATE certifications
SET status = 'REJECTED',
    rejection_reason = '请按当前发布要求，将经历叙述补充或精简至200字以内',
    reviewed_at = CURRENT_TIMESTAMP(6)
WHERE category = 'EXPERIENCE'
  AND status IN ('PENDING', 'APPROVED')
  AND (
      TRIM(COALESCE(description, '')) = ''
      OR CHAR_LENGTH(TRIM(description)) > 200
  );

UPDATE certifications
SET experience_business_type = 'MONETIZED'
WHERE category = 'EXPERIENCE';

ALTER TABLE certifications
    ADD CONSTRAINT chk_certification_unified_experience_type
        CHECK (
            category <> 'EXPERIENCE'
            OR COALESCE(experience_business_type, '') = 'MONETIZED'
        );

ALTER TABLE experience_invitation_rewards
    ADD COLUMN certification_id BIGINT NULL AFTER invitation_code,
    ADD COLUMN reward_amount DECIMAL(14, 2) NULL AFTER certification_id,
    ADD CONSTRAINT fk_experience_invitation_reward_certification
        FOREIGN KEY (certification_id) REFERENCES certifications(id),
    ADD INDEX idx_experience_invitation_reward_certification (certification_id);

UPDATE experience_invitation_rewards
SET certification_id = COALESCE(
        monetized_certification_id,
        public_welfare_certification_id
    ),
    reward_amount = total_reward_amount;

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

UPDATE home_banners
SET enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE deleted = FALSE
  AND action_type IN (
      'INVITE_PUBLIC_EXPERIENCE',
      'INVITE_MONETIZED_EXPERIENCE'
  );

UPDATE home_banners
SET display_mode = 'TEXT_ONLY',
    label_text = '首次发布奖励',
    title = '首次成功发布经历，得5元',
    description = '每个账号仅限首次',
    image_url = NULL,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE deleted = FALSE
  AND action_type = 'FIRST_EXPERIENCE_REWARD';

INSERT INTO home_banners (
    display_mode,
    image_url,
    action_type,
    sort_order,
    start_at,
    end_at,
    enabled,
    deleted
)
SELECT
    'IMAGE_ONLY',
    '/banners/invite-experience-01.png',
    'INVITE_EXPERIENCE',
    20,
    CURRENT_TIMESTAMP(6),
    '2099-12-31 23:59:59.999999',
    TRUE,
    FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM home_banners
    WHERE deleted = FALSE
      AND action_type = 'INVITE_EXPERIENCE'
);

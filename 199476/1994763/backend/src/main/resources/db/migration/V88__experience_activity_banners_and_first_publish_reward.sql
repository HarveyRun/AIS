CREATE TABLE first_experience_rewards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    certification_id BIGINT NOT NULL,
    experience_business_type VARCHAR(24) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_first_experience_reward_user UNIQUE (user_id),
    CONSTRAINT uk_first_experience_reward_certification UNIQUE (certification_id),
    CONSTRAINT fk_first_experience_reward_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_first_experience_reward_certification
        FOREIGN KEY (certification_id) REFERENCES certifications(id),
    CONSTRAINT chk_first_experience_reward_type
        CHECK (experience_business_type IN ('PUBLIC_WELFARE', 'MONETIZED')),
    CONSTRAINT chk_first_experience_reward_amount
        CHECK (
            (experience_business_type = 'PUBLIC_WELFARE' AND amount = 2.00)
            OR (experience_business_type = 'MONETIZED' AND amount = 5.00)
        ),
    INDEX idx_first_experience_reward_created_at (created_at)
);

INSERT INTO analytics_event_definitions (
    event_name,
    display_name,
    module_name,
    source_type
)
SELECT
    'first_experience_rewarded',
    '首次发布奖励到账',
    '活动',
    'SERVER'
WHERE NOT EXISTS (
    SELECT 1
    FROM analytics_event_definitions
    WHERE event_name = 'first_experience_rewarded'
);

UPDATE home_banners
SET enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE deleted = FALSE
  AND image_url IN (
      '/banners/invite-experience-01.png',
      '/banners/invite-experience-02-preview.png',
      '/banners/invite-experience-03-preview.png'
  );

INSERT INTO home_banners (
    display_mode,
    image_url,
    action_type,
    sort_order,
    enabled,
    deleted
)
SELECT
    'IMAGE_ONLY',
    '/banners/first-experience-reward.png',
    'FIRST_EXPERIENCE_REWARD',
    10,
    TRUE,
    FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM home_banners
    WHERE deleted = FALSE
      AND action_type = 'FIRST_EXPERIENCE_REWARD'
);

INSERT INTO home_banners (
    display_mode,
    image_url,
    action_type,
    sort_order,
    enabled,
    deleted
)
SELECT
    'IMAGE_ONLY',
    '/banners/invite-public-experience.png',
    'INVITE_PUBLIC_EXPERIENCE',
    20,
    TRUE,
    FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM home_banners
    WHERE deleted = FALSE
      AND action_type = 'INVITE_PUBLIC_EXPERIENCE'
);

INSERT INTO home_banners (
    display_mode,
    image_url,
    action_type,
    sort_order,
    enabled,
    deleted
)
SELECT
    'IMAGE_ONLY',
    '/banners/invite-monetized-experience.png',
    'INVITE_MONETIZED_EXPERIENCE',
    30,
    TRUE,
    FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM home_banners
    WHERE deleted = FALSE
      AND action_type = 'INVITE_MONETIZED_EXPERIENCE'
);

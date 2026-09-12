ALTER TABLE first_experience_rewards
    DROP CHECK chk_first_experience_reward_amount;

ALTER TABLE first_experience_rewards
    ADD CONSTRAINT chk_first_experience_reward_amount
        CHECK (
            (experience_business_type = 'PUBLIC_WELFARE' AND amount = 2.00)
            OR (
                experience_business_type = 'MONETIZED'
                AND amount IN (3.00, 5.00)
            )
        );

UPDATE home_banners
SET title = '首次成功发布经历，得3元',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE deleted = FALSE
  AND action_type = 'FIRST_EXPERIENCE_REWARD';

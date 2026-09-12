ALTER TABLE inquiries
    ADD COLUMN questioner_text_limit INT NOT NULL DEFAULT 50 AFTER paid_session_ends_at,
    ADD COLUMN answerer_text_limit INT NOT NULL DEFAULT 50 AFTER questioner_text_limit;

ALTER TABLE inquiry_audio_appointments
    ADD COLUMN actual_duration_seconds INT NOT NULL DEFAULT 0 AFTER duration_minutes,
    ADD COLUMN billable_seconds INT NOT NULL DEFAULT 0 AFTER actual_duration_seconds,
    ADD COLUMN active_segment_started_at DATETIME NULL AFTER billable_seconds,
    ADD COLUMN actual_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER amount,
    ADD COLUMN text_quota_granted TINYINT(1) NOT NULL DEFAULT 0 AFTER five_minute_warning_sent;

ALTER TABLE experience_tips
    ADD COLUMN fee_rate DECIMAL(7, 6) NOT NULL DEFAULT 0.000000 AFTER amount,
    ADD COLUMN fee_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER fee_rate,
    ADD COLUMN receiver_income_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER fee_amount;

UPDATE experience_tips
SET receiver_income_amount = amount
WHERE receiver_income_amount = 0.00;

ALTER TABLE first_experience_rewards
    ADD COLUMN fee_rate DECIMAL(7, 6) NOT NULL DEFAULT 0.000000 AFTER amount,
    ADD COLUMN fee_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER fee_rate,
    ADD COLUMN user_income_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER fee_amount;

UPDATE first_experience_rewards
SET user_income_amount = amount
WHERE user_income_amount = 0.00;

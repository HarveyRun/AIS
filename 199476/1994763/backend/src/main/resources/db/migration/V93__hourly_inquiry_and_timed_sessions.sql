ALTER TABLE users
    ADD COLUMN inquiry_hourly_rate INT NOT NULL DEFAULT 60 AFTER inquiry_price_updated_at,
    ADD COLUMN accepting_voice_inquiries BOOLEAN NOT NULL DEFAULT TRUE AFTER inquiry_hourly_rate,
    ADD COLUMN accepting_video_inquiries BOOLEAN NOT NULL DEFAULT TRUE AFTER accepting_voice_inquiries;

UPDATE users
SET inquiry_hourly_rate = GREATEST(1, LEAST(5000, inquiry_price_min));

ALTER TABLE inquiries
    ADD COLUMN flow_version INT NOT NULL DEFAULT 1 AFTER last_message_at,
    ADD COLUMN hourly_rate_snapshot INT NOT NULL DEFAULT 0 AFTER flow_version,
    ADD COLUMN voice_allowed BOOLEAN NOT NULL DEFAULT FALSE AFTER hourly_rate_snapshot,
    ADD COLUMN video_allowed BOOLEAN NOT NULL DEFAULT FALSE AFTER voice_allowed,
    ADD COLUMN session_type VARCHAR(20) NULL AFTER video_allowed,
    ADD COLUMN purchased_minutes INT NOT NULL DEFAULT 0 AFTER session_type,
    ADD COLUMN paid_session_started_at DATETIME(6) NULL AFTER purchased_minutes,
    ADD COLUMN paid_session_ends_at DATETIME(6) NULL AFTER paid_session_started_at,
    ADD INDEX idx_inquiry_paid_session_expiry (status, paid_session_ends_at);

ALTER TABLE inquiries
    DROP CHECK chk_inquiry_amount_positive_integer,
    ADD CONSTRAINT chk_inquiry_amount_non_negative CHECK (amount >= 0.00),
    ADD CONSTRAINT chk_inquiry_hourly_rate_snapshot CHECK (hourly_rate_snapshot >= 0 AND hourly_rate_snapshot <= 5000),
    ADD CONSTRAINT chk_inquiry_purchased_minutes CHECK (purchased_minutes >= 0);

ALTER TABLE inquiries
    ADD COLUMN conversation_expires_at DATETIME NULL AFTER accepted_at;

UPDATE inquiries
SET conversation_expires_at = DATE_ADD(COALESCE(accepted_at, created_at), INTERVAL 20 DAY)
WHERE flow_version >= 2
  AND status IN ('ACTIVE', 'TEXT_LIMIT_REACHED', 'TEXT_ENDED', 'PAID_ACTIVE')
  AND conversation_expires_at IS NULL;

ALTER TABLE inquiry_audio_appointments
    ADD COLUMN connect_deadline DATETIME NULL AFTER accepted_at,
    ADD COLUMN questioner_joined_at DATETIME NULL AFTER connect_deadline,
    ADD COLUMN answerer_joined_at DATETIME NULL AFTER questioner_joined_at,
    ADD COLUMN questioner_connected_at DATETIME NULL AFTER answerer_joined_at,
    ADD COLUMN answerer_connected_at DATETIME NULL AFTER questioner_connected_at,
    ADD COLUMN connected_at DATETIME NULL AFTER answerer_connected_at,
    ADD COLUMN last_disconnected_at DATETIME NULL AFTER connected_at,
    ADD COLUMN reconnect_deadline DATETIME NULL AFTER last_disconnected_at,
    ADD COLUMN end_reason VARCHAR(40) NULL AFTER reconnect_deadline,
    ADD COLUMN no_show_party VARCHAR(30) NULL AFTER end_reason;

ALTER TABLE feedback_records
    ADD COLUMN resolution VARCHAR(1000) NULL AFTER status,
    ADD COLUMN resolved_at DATETIME NULL AFTER resolution,
    ADD COLUMN handled_by_admin_id BIGINT NULL AFTER resolved_at,
    ADD CONSTRAINT fk_feedback_records_handled_admin
        FOREIGN KEY (handled_by_admin_id) REFERENCES admin_users(id);

ALTER TABLE users
    ADD COLUMN register_device_id VARCHAR(100) NULL AFTER register_location,
    ADD COLUMN last_login_device_id VARCHAR(100) NULL AFTER last_login_location;

ALTER TABLE first_experience_rewards
    ADD COLUMN risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW' AFTER amount,
    ADD COLUMN risk_reasons VARCHAR(1000) NULL AFTER risk_level;

ALTER TABLE withdrawals
    ADD COLUMN risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW' AFTER arrival_amount,
    ADD COLUMN risk_reasons VARCHAR(1000) NULL AFTER risk_level;

CREATE TABLE voice_call_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL,
    appointment_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    event_type VARCHAR(40) NOT NULL,
    detail VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_voice_call_events_appointment (appointment_id, created_at),
    INDEX idx_voice_call_events_type_time (event_type, created_at),
    CONSTRAINT fk_voice_call_events_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT fk_voice_call_events_appointment FOREIGN KEY (appointment_id) REFERENCES inquiry_audio_appointments(id),
    CONSTRAINT fk_voice_call_events_user FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE wallet_income_holds
    MODIFY COLUMN inquiry_id BIGINT NULL;

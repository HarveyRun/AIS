ALTER TABLE inquiry_audio_appointments
    ADD COLUMN previous_inquiry_status VARCHAR(30) NULL AFTER status,
    ADD COLUMN five_minute_warning_sent BOOLEAN NOT NULL DEFAULT FALSE AFTER previous_inquiry_status;

CREATE TABLE inquiry_audio_extensions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    questioner_id BIGINT NOT NULL,
    duration_minutes INT NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    frozen_recharge_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    frozen_income_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    request_id VARCHAR(80) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_audio_extension_appointment
        FOREIGN KEY (appointment_id) REFERENCES inquiry_audio_appointments(id),
    CONSTRAINT fk_audio_extension_questioner
        FOREIGN KEY (questioner_id) REFERENCES users(id),
    CONSTRAINT uq_audio_extension_request UNIQUE (questioner_id, request_id),
    INDEX idx_audio_extension_appointment (appointment_id, id)
);

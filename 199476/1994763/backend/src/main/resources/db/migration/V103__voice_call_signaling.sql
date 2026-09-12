CREATE TABLE inquiry_voice_signals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL,
    appointment_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    signal_type VARCHAR(20) NOT NULL,
    payload MEDIUMTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_voice_signal_inquiry
        FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT fk_voice_signal_appointment
        FOREIGN KEY (appointment_id) REFERENCES inquiry_audio_appointments(id),
    CONSTRAINT fk_voice_signal_sender
        FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_voice_signal_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id),
    INDEX idx_voice_signal_recipient (appointment_id, recipient_id, id),
    INDEX idx_voice_signal_created (created_at)
);

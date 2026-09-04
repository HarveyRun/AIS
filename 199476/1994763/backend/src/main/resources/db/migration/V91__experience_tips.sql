CREATE TABLE experience_tips (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payer_user_id BIGINT NOT NULL,
    receiver_user_id BIGINT NOT NULL,
    certification_id BIGINT NOT NULL,
    request_no VARCHAR(64) NOT NULL,
    experience_business_type VARCHAR(24) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_experience_tip_payer_request UNIQUE (payer_user_id, request_no),
    CONSTRAINT fk_experience_tip_payer FOREIGN KEY (payer_user_id) REFERENCES users(id),
    CONSTRAINT fk_experience_tip_receiver FOREIGN KEY (receiver_user_id) REFERENCES users(id),
    CONSTRAINT fk_experience_tip_certification FOREIGN KEY (certification_id) REFERENCES certifications(id),
    CONSTRAINT chk_experience_tip_users CHECK (payer_user_id <> receiver_user_id),
    CONSTRAINT chk_experience_tip_amount CHECK (amount >= 1.00 AND amount <= 5000.00),
    INDEX idx_experience_tip_receiver_time (receiver_user_id, created_at),
    INDEX idx_experience_tip_certification_time (certification_id, created_at)
);

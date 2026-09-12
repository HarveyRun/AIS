ALTER TABLE feedback_records
    ADD COLUMN inquiry_id BIGINT NULL AFTER target_user_id,
    ADD CONSTRAINT fk_feedback_inquiry
        FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    ADD UNIQUE KEY uk_feedback_user_inquiry_type (user_id, inquiry_id, feedback_type);

CREATE TABLE user_communication_blocks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_low_id BIGINT NOT NULL,
    user_high_id BIGINT NOT NULL,
    blocked_by_user_id BIGINT NOT NULL,
    source_inquiry_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_communication_block_pair (user_low_id, user_high_id),
    KEY idx_user_communication_blocks_high (user_high_id),
    KEY idx_user_communication_blocks_blocked_by (blocked_by_user_id),
    KEY idx_user_communication_blocks_inquiry (source_inquiry_id),
    CONSTRAINT fk_user_blocks_low FOREIGN KEY (user_low_id) REFERENCES users(id),
    CONSTRAINT fk_user_blocks_high FOREIGN KEY (user_high_id) REFERENCES users(id),
    CONSTRAINT fk_user_blocks_blocked_by FOREIGN KEY (blocked_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_user_blocks_inquiry FOREIGN KEY (source_inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT chk_user_communication_block_pair CHECK (user_low_id < user_high_id)
);

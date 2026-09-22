CREATE TABLE experience_proof_uploads (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    oss_upload_id VARCHAR(256) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    expected_size BIGINT NOT NULL,
    part_size INT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    consumed_at DATETIME(6) NULL,
    CONSTRAINT fk_experience_proof_upload_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_experience_proof_storage_key (storage_key),
    INDEX idx_experience_proof_user_status (user_id, status),
    INDEX idx_experience_proof_expiry (status, expires_at)
);

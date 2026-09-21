CREATE TABLE experience_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    certification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_experience_like_certification
        FOREIGN KEY (certification_id) REFERENCES certifications(id),
    CONSTRAINT fk_experience_like_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_experience_like_user UNIQUE (certification_id, user_id),
    INDEX idx_experience_like_count (certification_id, active),
    INDEX idx_experience_like_user (user_id, active)
);

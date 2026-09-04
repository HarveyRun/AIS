CREATE TABLE experience_invitation_relationships (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lower_user_id BIGINT NOT NULL,
    higher_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_experience_invitation_relationship_pair
        UNIQUE (lower_user_id, higher_user_id),
    CONSTRAINT fk_experience_invitation_relationship_lower_user
        FOREIGN KEY (lower_user_id) REFERENCES users(id),
    CONSTRAINT fk_experience_invitation_relationship_higher_user
        FOREIGN KEY (higher_user_id) REFERENCES users(id),
    CONSTRAINT chk_experience_invitation_relationship_order
        CHECK (lower_user_id < higher_user_id)
);

INSERT IGNORE INTO experience_invitation_relationships (
    lower_user_id,
    higher_user_id,
    created_at
)
SELECT
    LEAST(claimant_user_id, invited_user_id),
    GREATEST(claimant_user_id, invited_user_id),
    MIN(created_at)
FROM experience_invitation_rewards
GROUP BY
    LEAST(claimant_user_id, invited_user_id),
    GREATEST(claimant_user_id, invited_user_id);

CREATE TABLE invitation_submission_guards (
    user_id BIGINT PRIMARY KEY,
    invalid_attempts INT NOT NULL DEFAULT 0,
    last_invalid_at DATETIME(6) NULL,
    cleared_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_invitation_submission_guard_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_invitation_submission_guard_attempts
        CHECK (invalid_attempts >= 0)
);

UPDATE home_banners
SET action_type = 'NONE',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE action_type = 'BASIC_CERTIFICATION';

CREATE TABLE experience_invitation_rewards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    claimant_user_id BIGINT NOT NULL,
    invited_user_id BIGINT NOT NULL,
    invitation_code VARCHAR(20) NOT NULL,
    public_welfare_certification_id BIGINT NULL,
    monetized_certification_id BIGINT NULL,
    public_welfare_reward_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    monetized_reward_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    total_reward_amount DECIMAL(14, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_experience_invitation_reward_invited_user UNIQUE (invited_user_id),
    CONSTRAINT fk_experience_invitation_reward_claimant
        FOREIGN KEY (claimant_user_id) REFERENCES users(id),
    CONSTRAINT fk_experience_invitation_reward_invited
        FOREIGN KEY (invited_user_id) REFERENCES users(id),
    CONSTRAINT fk_experience_invitation_reward_public_certification
        FOREIGN KEY (public_welfare_certification_id) REFERENCES certifications(id),
    CONSTRAINT fk_experience_invitation_reward_monetized_certification
        FOREIGN KEY (monetized_certification_id) REFERENCES certifications(id),
    CONSTRAINT chk_experience_invitation_reward_users
        CHECK (claimant_user_id <> invited_user_id),
    CONSTRAINT chk_experience_invitation_reward_public_amount
        CHECK (public_welfare_reward_amount IN (0.00, 2.00)),
    CONSTRAINT chk_experience_invitation_reward_monetized_amount
        CHECK (monetized_reward_amount IN (0.00, 5.00)),
    CONSTRAINT chk_experience_invitation_reward_total
        CHECK (
            total_reward_amount > 0.00
            AND total_reward_amount = public_welfare_reward_amount + monetized_reward_amount
        ),
    INDEX idx_experience_invitation_reward_claimant_time (claimant_user_id, created_at)
);

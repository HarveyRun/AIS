CREATE TABLE invitation_campaign_settings (
    id BIGINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    reward_amount DECIMAL(14, 2) NOT NULL DEFAULT 3.00,
    updated_by_admin_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_invitation_campaign_admin
        FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(id)
);

INSERT INTO invitation_campaign_settings(id, enabled, reward_amount)
VALUES (1, FALSE, 3.00);

CREATE TABLE user_invitations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inviter_user_id BIGINT NOT NULL,
    invitee_user_id BIGINT NOT NULL,
    invitation_code VARCHAR(20) NOT NULL,
    inviter_real_name VARCHAR(30) NOT NULL,
    reward_amount DECIMAL(14, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    review_reason VARCHAR(300) NULL,
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    rewarded_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_user_invitation_invitee UNIQUE (invitee_user_id),
    CONSTRAINT fk_user_invitation_inviter
        FOREIGN KEY (inviter_user_id) REFERENCES users(id),
    CONSTRAINT fk_user_invitation_invitee
        FOREIGN KEY (invitee_user_id) REFERENCES users(id),
    CONSTRAINT fk_user_invitation_review_admin
        FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_user_invitation_inviter (inviter_user_id, id),
    INDEX idx_user_invitation_status (status, id)
);

INSERT INTO admin_permissions(code, name, module_name, action_name, sort_order, system_permission)
VALUES
    ('INVITATION_CAMPAIGN_VIEW', '查看邀请答主活动', '邀请答主活动', '查看', 1120, TRUE),
    ('INVITATION_CAMPAIGN_EDIT', '上下架邀请答主活动', '邀请答主活动', '上架/下架', 1130, TRUE);

INSERT INTO admin_permissions(code, name, module_name, action_name, sort_order, system_permission)
VALUES
    ('INVITATION_REVIEW_VIEW', '查看邀请审核', '邀请审核', '查看', 1140, TRUE),
    ('INVITATION_REVIEW', '审核邀请', '邀请审核', '审核', 1150, TRUE);

INSERT IGNORE INTO admin_role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
JOIN admin_permissions p ON p.code IN ('INVITATION_CAMPAIGN_VIEW', 'INVITATION_CAMPAIGN_EDIT')
WHERE r.code IN ('SUPER_ADMIN', 'GENERAL_ADMIN_L1', 'GENERAL_ADMIN_L2', 'CONTENT_ADMIN');

INSERT IGNORE INTO admin_role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
JOIN admin_permissions p ON p.code IN ('INVITATION_REVIEW_VIEW', 'INVITATION_REVIEW')
WHERE r.code IN ('SUPER_ADMIN', 'GENERAL_ADMIN_L1', 'GENERAL_ADMIN_L2', 'CERTIFICATION_ADMIN');

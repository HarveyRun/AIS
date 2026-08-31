ALTER TABLE home_banners
    ADD COLUMN action_type VARCHAR(40) NOT NULL DEFAULT 'RANDOM_DISCOVERY' AFTER image_url;

CREATE TABLE content_contributions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    matter_name VARCHAR(40) NOT NULL,
    raw_payload_encrypted TEXT NOT NULL,
    candidate_fingerprint CHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    standard_matter_name VARCHAR(40) NULL,
    standard_matter_key VARCHAR(80) NULL,
    reward_amount DECIMAL(14,2) NULL,
    review_reason VARCHAR(500) NULL,
    violation_level TINYINT NULL,
    violation_reason VARCHAR(500) NULL,
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    version_no BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_content_contribution_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_content_contribution_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_content_contribution_user (user_id, created_at, id),
    INDEX idx_content_contribution_status (status, created_at, id),
    INDEX idx_content_contribution_candidate (candidate_fingerprint, status, created_at, id),
    INDEX idx_content_contribution_standard (standard_matter_key, status, created_at, id)
);

CREATE TABLE content_contribution_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contribution_id BIGINT NOT NULL,
    job_name VARCHAR(40) NOT NULL,
    responsibility VARCHAR(300) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_content_contribution_job FOREIGN KEY (contribution_id)
        REFERENCES content_contributions(id),
    UNIQUE KEY uk_content_contribution_job_order (contribution_id, sort_order),
    INDEX idx_content_contribution_job_parent (contribution_id, id)
);

CREATE TABLE content_contribution_rewards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contribution_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    standard_matter_key VARCHAR(80) NOT NULL,
    candidate_fingerprint CHAR(64) NOT NULL,
    reward_amount DECIMAL(14,2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_content_contribution_reward_contribution FOREIGN KEY (contribution_id)
        REFERENCES content_contributions(id),
    CONSTRAINT fk_content_contribution_reward_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_content_contribution_reward_record (contribution_id),
    UNIQUE KEY uk_content_contribution_reward_user_matter (user_id, standard_matter_key),
    UNIQUE KEY uk_content_contribution_reward_candidate (candidate_fingerprint)
);

INSERT INTO admin_permissions(code,name,module_name,action_name,sort_order,system_permission) VALUES
('CONTENT_CONTRIBUTION_VIEW','查看内容共建','内容共建','查看',760,TRUE),
('CONTENT_CONTRIBUTION_REVIEW','审核内容共建','内容共建','整体审核',761,TRUE),
('CONTENT_CONTRIBUTION_VIOLATION','标记共建违规','内容共建','违规处理',762,TRUE);

INSERT IGNORE INTO admin_role_permissions(role_id,permission_id)
SELECT role.id,permission.id FROM admin_roles role JOIN admin_permissions permission
WHERE permission.code IN (
    'CONTENT_CONTRIBUTION_VIEW','CONTENT_CONTRIBUTION_REVIEW','CONTENT_CONTRIBUTION_VIOLATION'
) AND role.code IN ('SUPER_ADMIN','GENERAL_ADMIN_L1','GENERAL_ADMIN_L2','CONTENT_ADMIN');

INSERT INTO home_banners(
    display_mode,label_text,title,description,image_url,action_type,sort_order,enabled
)
SELECT
    'TEXT_ONLY','内容共建','一起补充大家可能遇到的事','内容采用可得1～3元奖励',NULL,
    'CONTENT_CONTRIBUTION',1,TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM home_banners
    WHERE action_type='CONTENT_CONTRIBUTION' AND deleted=FALSE
);

INSERT INTO analytics_event_definitions(event_name,display_name,module_name,source_type) VALUES
('content_contribution_open','进入内容共建','内容共建','CLIENT'),
('content_contribution_preview','预览共建内容','内容共建','CLIENT'),
('content_contribution_submit','提交共建内容','内容共建','SERVER'),
('content_contribution_adopted','共建内容采用','内容共建','SERVER'),
('content_contribution_rejected','共建内容驳回','内容共建','SERVER'),
('content_contribution_rewarded','共建奖励到账','内容共建','SERVER');

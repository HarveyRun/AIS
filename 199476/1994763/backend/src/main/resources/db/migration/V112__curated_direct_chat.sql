CREATE TABLE curated_membership_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    identity_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    job_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    identity_rejection_reason VARCHAR(500) NULL,
    job_rejection_reason VARCHAR(500) NULL,
    real_name_ciphertext VARCHAR(512) NULL,
    id_card_ciphertext VARCHAR(512) NULL,
    id_card_fingerprint CHAR(64) NULL,
    job_title VARCHAR(80) NULL,
    job_years INT NULL,
    identity_reviewed_by BIGINT NULL,
    identity_reviewed_at DATETIME(6) NULL,
    job_reviewed_by BIGINT NULL,
    job_reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_curated_application_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_curated_identity_reviewer FOREIGN KEY (identity_reviewed_by) REFERENCES admin_users(id),
    CONSTRAINT fk_curated_job_reviewer FOREIGN KEY (job_reviewed_by) REFERENCES admin_users(id),
    CONSTRAINT chk_curated_job_years CHECK (job_years IS NULL OR job_years >= 0),
    UNIQUE KEY uk_curated_application_user (user_id),
    UNIQUE KEY uk_curated_application_id_card (id_card_fingerprint),
    KEY idx_curated_application_status (identity_status, job_status, updated_at)
);

CREATE TABLE curated_membership_materials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    material_type VARCHAR(24) NOT NULL,
    media_type VARCHAR(16) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_curated_material_application FOREIGN KEY (application_id) REFERENCES curated_membership_applications(id),
    KEY idx_curated_material_application (application_id, material_type, deleted_at, id)
);

CREATE TABLE curated_memberships (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'INACTIVE',
    started_at DATETIME(6) NULL,
    expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_curated_membership_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_curated_membership_application FOREIGN KEY (application_id) REFERENCES curated_membership_applications(id),
    UNIQUE KEY uk_curated_membership_user (user_id),
    KEY idx_curated_membership_active (status, expires_at)
);

CREATE TABLE curated_membership_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    request_no VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    channel VARCHAR(24) NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    status VARCHAR(24) NOT NULL,
    provider_trade_no VARCHAR(128) NULL,
    paid_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_curated_order_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_curated_order_membership FOREIGN KEY (membership_id) REFERENCES curated_memberships(id),
    CONSTRAINT chk_curated_order_amount CHECK (amount > 0),
    UNIQUE KEY uk_curated_order_request (user_id, request_no),
    UNIQUE KEY uk_curated_order_no (order_no),
    UNIQUE KEY uk_curated_order_trade_no (provider_trade_no),
    KEY idx_curated_order_status (status, created_at)
);

CREATE TABLE curated_conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_low_id BIGINT NOT NULL,
    user_high_id BIGINT NOT NULL,
    last_message_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_curated_conversation_low FOREIGN KEY (user_low_id) REFERENCES users(id),
    CONSTRAINT fk_curated_conversation_high FOREIGN KEY (user_high_id) REFERENCES users(id),
    CONSTRAINT chk_curated_conversation_pair CHECK (user_low_id < user_high_id),
    UNIQUE KEY uk_curated_conversation_pair (user_low_id, user_high_id),
    KEY idx_curated_conversation_low_time (user_low_id, last_message_at),
    KEY idx_curated_conversation_high_time (user_high_id, last_message_at)
);

CREATE TABLE curated_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NULL,
    message_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    raw_content_encrypted TEXT NULL,
    attachment_key VARCHAR(500) NULL,
    attachment_name VARCHAR(255) NULL,
    attachment_size BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_curated_message_conversation FOREIGN KEY (conversation_id) REFERENCES curated_conversations(id),
    CONSTRAINT fk_curated_message_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    KEY idx_curated_message_conversation (conversation_id, id)
);

CREATE TABLE curated_conversation_reads (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_curated_read_conversation FOREIGN KEY (conversation_id) REFERENCES curated_conversations(id),
    CONSTRAINT fk_curated_read_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_curated_read_message FOREIGN KEY (last_read_message_id) REFERENCES curated_messages(id)
);

CREATE TABLE curated_chat_blocks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_curated_block_blocker FOREIGN KEY (blocker_id) REFERENCES users(id),
    CONSTRAINT fk_curated_block_blocked FOREIGN KEY (blocked_id) REFERENCES users(id),
    CONSTRAINT chk_curated_block_users CHECK (blocker_id <> blocked_id),
    UNIQUE KEY uk_curated_block_direction (blocker_id, blocked_id),
    KEY idx_curated_block_blocked (blocked_id)
);

CREATE TABLE curated_voice_calls (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    caller_id BIGINT NOT NULL,
    callee_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    connect_deadline DATETIME(6) NOT NULL,
    answered_at DATETIME(6) NULL,
    connected_at DATETIME(6) NULL,
    ended_at DATETIME(6) NULL,
    ended_by_user_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_curated_call_conversation FOREIGN KEY (conversation_id) REFERENCES curated_conversations(id),
    CONSTRAINT fk_curated_call_caller FOREIGN KEY (caller_id) REFERENCES users(id),
    CONSTRAINT fk_curated_call_callee FOREIGN KEY (callee_id) REFERENCES users(id),
    CONSTRAINT fk_curated_call_ended_by FOREIGN KEY (ended_by_user_id) REFERENCES users(id),
    KEY idx_curated_call_conversation (conversation_id, created_at),
    KEY idx_curated_call_timeout (status, connect_deadline)
);

CREATE TABLE curated_voice_signals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    call_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    signal_type VARCHAR(24) NOT NULL,
    payload_json TEXT NOT NULL,
    consumed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_curated_signal_call FOREIGN KEY (call_id) REFERENCES curated_voice_calls(id),
    CONSTRAINT fk_curated_signal_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_curated_signal_recipient FOREIGN KEY (recipient_id) REFERENCES users(id),
    KEY idx_curated_signal_pending (call_id, recipient_id, consumed_at, id)
);

INSERT INTO admin_permissions(code,name,module_name,action_name,sort_order,system_permission) VALUES
('CURATED_CHAT_VIEW','查看严选直聊','严选直聊','查看',2050,TRUE),
('CURATED_CHAT_IDENTITY_REVIEW','审核严选实名','严选直聊','实名审核',2060,TRUE),
('CURATED_CHAT_JOB_REVIEW','审核严选岗位','严选直聊','岗位审核',2070,TRUE),
('CURATED_CHAT_MEMBER_MANAGE','管理严选会员','严选直聊','会员管理',2080,TRUE);

INSERT IGNORE INTO admin_role_permissions(role_id,permission_id)
SELECT role.id,permission.id
FROM admin_roles role
JOIN admin_permissions permission ON permission.code LIKE 'CURATED_CHAT_%'
WHERE role.code IN ('SUPER_ADMIN','GENERAL_ADMIN_L1','CERTIFICATION_ADMIN');

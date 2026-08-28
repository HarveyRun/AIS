ALTER TABLE inquiries
    ADD COLUMN settleable_amount DECIMAL(14,2) NULL AFTER amount,
    ADD COLUMN timeout_refunded_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER settleable_amount,
    ADD COLUMN timeout_refunded_recharge_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER timeout_refunded_amount,
    ADD COLUMN timeout_refunded_income_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER timeout_refunded_recharge_amount,
    ADD COLUMN timeout_count TINYINT NOT NULL DEFAULT 0 AFTER timeout_refunded_income_amount,
    ADD COLUMN first_questioner_message_at DATETIME(6) NULL AFTER accepted_at,
    ADD COLUMN first_answerer_reply_at DATETIME(6) NULL AFTER first_questioner_message_at,
    ADD COLUMN reply_cycle_started_at DATETIME(6) NULL AFTER first_answerer_reply_at,
    ADD COLUMN reply_deadline DATETIME(6) NULL AFTER reply_cycle_started_at,
    ADD COLUMN end_requested_at DATETIME(6) NULL AFTER reply_deadline,
    ADD COLUMN end_reminder_stage TINYINT NOT NULL DEFAULT 0 AFTER end_requested_at,
    ADD COLUMN question_raw_encrypted MEDIUMTEXT NULL AFTER question,
    ADD INDEX idx_inquiry_reply_timeout (status, reply_deadline),
    ADD INDEX idx_inquiry_end_reminder (status, confirmation_deadline, end_reminder_stage);

UPDATE inquiries SET settleable_amount=amount WHERE settleable_amount IS NULL;

ALTER TABLE inquiries
    MODIFY COLUMN settleable_amount DECIMAL(14,2) NOT NULL;

ALTER TABLE inquiry_messages
    MODIFY COLUMN sender_id BIGINT NULL,
    ADD COLUMN raw_content_encrypted MEDIUMTEXT NULL AFTER content;

CREATE TABLE inquiry_timeout_refunds (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL,
    timeout_no TINYINT NOT NULL,
    cycle_started_at DATETIME(6) NOT NULL,
    deadline_at DATETIME(6) NOT NULL,
    refund_amount DECIMAL(14,2) NOT NULL,
    recharge_refund_amount DECIMAL(14,2) NOT NULL,
    income_refund_amount DECIMAL(14,2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inquiry_timeout_refund_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    UNIQUE KEY uk_inquiry_timeout_number (inquiry_id, timeout_no),
    INDEX idx_inquiry_timeout_created (created_at, id)
);

CREATE TABLE inquiry_end_disputes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL UNIQUE,
    trigger_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision VARCHAR(40) NULL,
    decision_reason VARCHAR(500) NULL,
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_end_dispute_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT fk_end_dispute_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_end_dispute_status (status, created_at, id)
);

CREATE TABLE inquiry_message_report_cases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_message_report_case_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT fk_message_report_case_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_message_report_case_status (status, created_at, id)
);

CREATE TABLE inquiry_message_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    inquiry_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reported_user_id BIGINT NOT NULL,
    report_type VARCHAR(40) NOT NULL,
    violation_level TINYINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision_reason VARCHAR(500) NULL,
    masked_content_snapshot TEXT NOT NULL,
    raw_content_snapshot_encrypted MEDIUMTEXT NULL,
    question_snapshot VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at DATETIME(6) NULL,
    CONSTRAINT fk_message_report_case FOREIGN KEY (case_id) REFERENCES inquiry_message_report_cases(id),
    CONSTRAINT fk_message_report_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT fk_message_report_message FOREIGN KEY (message_id) REFERENCES inquiry_messages(id),
    CONSTRAINT fk_message_report_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_message_report_reported FOREIGN KEY (reported_user_id) REFERENCES users(id),
    UNIQUE KEY uk_message_reporter_message (reporter_id, message_id),
    INDEX idx_message_report_case (case_id, id),
    INDEX idx_message_report_daily (reporter_id, created_at)
);

CREATE TABLE inquiry_dispute_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    actor_id BIGINT NULL,
    detail VARCHAR(1000) NOT NULL,
    evidence_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inquiry_dispute_event_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    INDEX idx_dispute_event_inquiry (inquiry_id, created_at, id)
);

CREATE TABLE user_violation_counters (
    user_id BIGINT NOT NULL,
    violation_level TINYINT NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    threshold_count INT NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, violation_level),
    CONSTRAINT fk_violation_counter_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_violation_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    inquiry_id BIGINT NULL,
    source_type VARCHAR(40) NOT NULL,
    source_id BIGINT NOT NULL,
    violation_level TINYINT NOT NULL,
    fact_description VARCHAR(500) NOT NULL,
    rule_basis VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_violation_record_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_violation_record_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    UNIQUE KEY uk_violation_source_user (source_type, source_id, user_id),
    INDEX idx_violation_record_user (user_id, created_at, id),
    INDEX idx_violation_record_inquiry (inquiry_id, user_id, violation_level)
);

CREATE TABLE user_inquiry_violation_summaries (
    user_id BIGINT NOT NULL,
    inquiry_id BIGINT NOT NULL,
    violation_level TINYINT NOT NULL,
    source_record_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, inquiry_id),
    CONSTRAINT fk_violation_summary_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_violation_summary_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT fk_violation_summary_record FOREIGN KEY (source_record_id) REFERENCES user_violation_records(id)
);

CREATE TABLE user_risk_watchlist (
    user_id BIGINT PRIMARY KEY,
    risk_score DECIMAL(10,4) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WATCHING',
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_risk_watch_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_risk_watch_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_risk_watch_status (status, risk_score, updated_at)
);

INSERT INTO admin_permissions(code,name,module_name,action_name,sort_order,system_permission) VALUES
('INQUIRY_DISPUTE_VIEW','查看询问纠纷','询问纠纷','查看',350,TRUE),
('INQUIRY_DISPUTE_PROCESS','处理结束纠纷','询问纠纷','处理结束纠纷',351,TRUE),
('MESSAGE_REPORT_PROCESS','处理消息举报','询问纠纷','处理消息举报',352,TRUE),
('SENSITIVE_ORIGINAL_VIEW','查看敏感原文','询问纠纷','查看敏感原文',353,TRUE),
('RISK_WATCH_VIEW','查看综合风险关注','询问纠纷','查看风险关注',354,TRUE),
('RISK_WATCH_PROCESS','处理综合风险关注','询问纠纷','处理风险关注',355,TRUE);

INSERT IGNORE INTO admin_role_permissions(role_id,permission_id)
SELECT role.id,permission.id FROM admin_roles role JOIN admin_permissions permission
WHERE permission.code IN (
    'INQUIRY_DISPUTE_VIEW','INQUIRY_DISPUTE_PROCESS','MESSAGE_REPORT_PROCESS',
    'SENSITIVE_ORIGINAL_VIEW','RISK_WATCH_VIEW','RISK_WATCH_PROCESS'
) AND (
    role.code='SUPER_ADMIN'
    OR role.code='GENERAL_ADMIN_L1'
    OR (role.code='GENERAL_ADMIN_L2' AND permission.code IN (
        'INQUIRY_DISPUTE_VIEW','INQUIRY_DISPUTE_PROCESS','MESSAGE_REPORT_PROCESS','RISK_WATCH_VIEW'
    ))
    OR (role.code='CUSTOMER_SERVICE_ADMIN' AND permission.code IN (
        'INQUIRY_DISPUTE_VIEW','MESSAGE_REPORT_PROCESS','RISK_WATCH_VIEW'
    ))
);

CREATE TABLE fund_vouchers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    voucher_no VARCHAR(64) NOT NULL UNIQUE,
    business_type VARCHAR(40) NOT NULL,
    business_id VARCHAR(80) NOT NULL,
    action_code VARCHAR(50) NOT NULL,
    description VARCHAR(300) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_fund_voucher_business (business_type, business_id, action_code),
    INDEX idx_fund_voucher_time (occurred_at, id)
);

CREATE TABLE fund_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    voucher_id BIGINT NOT NULL,
    account_code VARCHAR(80) NOT NULL,
    user_id BIGINT NULL,
    signed_amount DECIMAL(14,2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_fund_entry_voucher FOREIGN KEY (voucher_id) REFERENCES fund_vouchers(id),
    CONSTRAINT fk_fund_entry_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_fund_entry_account (account_code, created_at),
    INDEX idx_fund_entry_user (user_id, created_at)
);

CREATE TABLE channel_bill_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider VARCHAR(20) NOT NULL,
    bill_type VARCHAR(30) NOT NULL,
    bill_date DATE NOT NULL,
    source_hash VARCHAR(64) NOT NULL UNIQUE,
    provider_trade_no VARCHAR(100) NULL,
    merchant_order_no VARCHAR(100) NULL,
    business_type VARCHAR(60) NULL,
    trade_status VARCHAR(40) NULL,
    gross_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    net_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    channel_fee DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    occurred_at DATETIME(6) NULL,
    raw_content TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_channel_bill_order (merchant_order_no, provider_trade_no),
    INDEX idx_channel_bill_date (provider, bill_type, bill_date)
);

CREATE TABLE reconciliation_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider VARCHAR(20) NOT NULL,
    bill_type VARCHAR(30) NOT NULL,
    bill_date DATE NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    record_count INT NOT NULL DEFAULT 0,
    matched_count INT NOT NULL DEFAULT 0,
    difference_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(500) NULL,
    started_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    created_by_admin_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reconciliation_task_admin FOREIGN KEY (created_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_reconciliation_task_date (bill_date, status, id)
);

CREATE TABLE reconciliation_differences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NULL,
    difference_type VARCHAR(40) NOT NULL,
    business_type VARCHAR(40) NULL,
    business_id VARCHAR(100) NULL,
    provider_trade_no VARCHAR(100) NULL,
    expected_amount DECIMAL(14,2) NULL,
    actual_amount DECIMAL(14,2) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    detail VARCHAR(500) NULL,
    resolution VARCHAR(500) NULL,
    resolved_by_admin_id BIGINT NULL,
    resolved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reconciliation_difference_task FOREIGN KEY (task_id) REFERENCES reconciliation_tasks(id),
    CONSTRAINT fk_reconciliation_difference_admin FOREIGN KEY (resolved_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_reconciliation_difference_status (status, difference_type, id)
);

CREATE TABLE inquiry_evaluations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL UNIQUE,
    questioner_id BIGINT NOT NULL,
    answered_level TINYINT NOT NULL,
    specific_level TINYINT NOT NULL,
    matched_level TINYINT NOT NULL,
    useful_level TINYINT NOT NULL,
    communication_level TINYINT NOT NULL,
    ask_again_level TINYINT NOT NULL,
    negative_tags JSON NULL,
    comment VARCHAR(300) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inquiry_evaluation_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT fk_inquiry_evaluation_user FOREIGN KEY (questioner_id) REFERENCES users(id),
    INDEX idx_inquiry_evaluation_user (questioner_id, created_at)
);

CREATE TABLE inquiry_quality_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL UNIQUE,
    questioner_id BIGINT NOT NULL,
    answerer_id BIGINT NOT NULL,
    reason_code VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    decision VARCHAR(30) NULL,
    decision_reason VARCHAR(500) NULL,
    penalty_duration VARCHAR(30) NULL,
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_quality_review_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT fk_quality_review_questioner FOREIGN KEY (questioner_id) REFERENCES users(id),
    CONSTRAINT fk_quality_review_answerer FOREIGN KEY (answerer_id) REFERENCES users(id),
    CONSTRAINT fk_quality_review_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_quality_review_status (status, created_at, id)
);

ALTER TABLE platform_fee_records
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'EARNED' AFTER answerer_income_amount,
    ADD COLUMN finalized_at DATETIME(6) NULL AFTER status;

UPDATE platform_fee_records SET finalized_at=created_at WHERE status='EARNED';

ALTER TABLE withdrawals
    ADD COLUMN result_reason VARCHAR(300) NULL AFTER status,
    ADD COLUMN result_imported_at DATETIME(6) NULL AFTER exported_at;

INSERT INTO admin_permissions(code,name,module_name,action_name,sort_order,system_permission) VALUES
('FINANCE_RECONCILIATION_VIEW','查看资金对账','资金对账','查看',430,TRUE),
('FINANCE_RECONCILIATION_RUN','执行资金对账','资金对账','执行',431,TRUE),
('FINANCE_RECONCILIATION_IMPORT','导入资金结果','资金对账','导入',432,TRUE),
('FINANCE_RECONCILIATION_RESOLVE','处理资金差错','资金对账','处理',433,TRUE),
('ANSWER_QUALITY_VIEW','查看回答质量','回答质量','查看',340,TRUE),
('ANSWER_QUALITY_REVIEW','处理质量复核','回答质量','复核',341,TRUE),
('ANSWER_QUALITY_EVIDENCE','查看复核交流记录','回答质量','查看证据',342,TRUE),
('ANSWER_QUALITY_PENALIZE','执行质量处罚','回答质量','处罚',343,TRUE);

INSERT IGNORE INTO admin_role_permissions(role_id,permission_id)
SELECT role.id,permission.id FROM admin_roles role JOIN admin_permissions permission
WHERE permission.code IN (
    'FINANCE_RECONCILIATION_VIEW','FINANCE_RECONCILIATION_RUN','FINANCE_RECONCILIATION_IMPORT','FINANCE_RECONCILIATION_RESOLVE',
    'ANSWER_QUALITY_VIEW','ANSWER_QUALITY_REVIEW','ANSWER_QUALITY_EVIDENCE','ANSWER_QUALITY_PENALIZE'
) AND (
    role.code='SUPER_ADMIN'
    OR role.code='GENERAL_ADMIN_L1'
    OR (role.code='FINANCE_ADMIN' AND permission.code LIKE 'FINANCE_RECONCILIATION_%')
    OR (role.code='GENERAL_ADMIN_L2' AND permission.code IN ('ANSWER_QUALITY_VIEW','ANSWER_QUALITY_REVIEW','ANSWER_QUALITY_EVIDENCE'))
    OR (role.code='CUSTOMER_SERVICE_ADMIN' AND permission.code IN ('ANSWER_QUALITY_VIEW','ANSWER_QUALITY_REVIEW','ANSWER_QUALITY_EVIDENCE'))
);

INSERT INTO analytics_event_definitions(event_name,display_name,module_name,source_type) VALUES
('inquiry_evaluated','提交询问评价','回答质量','SERVER'),
('quality_review_requested','申请质量复核','回答质量','SERVER'),
('quality_review_refunded','质量复核退款','回答质量','SERVER'),
('quality_review_settled','质量复核维持结算','回答质量','SERVER'),
('reconciliation_completed','完成资金对账','资金','SERVER');

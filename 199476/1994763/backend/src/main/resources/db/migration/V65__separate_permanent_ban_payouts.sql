CREATE TABLE permanent_ban_payouts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    payee_name_snapshot VARCHAR(80) NULL,
    alipay_identifier_type_snapshot VARCHAR(20) NULL,
    alipay_account_ciphertext_snapshot VARCHAR(512) NULL,
    alipay_account_masked_snapshot VARCHAR(120) NULL,
    status VARCHAR(30) NOT NULL,
    batch_no VARCHAR(64) NULL,
    result_reason VARCHAR(300) NULL,
    banned_at DATETIME(6) NOT NULL,
    due_at DATETIME(6) NOT NULL,
    exported_at DATETIME(6) NULL,
    result_imported_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_permanent_ban_payout_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_permanent_ban_payout_amount CHECK (amount > 0.00),
    INDEX idx_permanent_ban_payout_status (status, due_at, id),
    INDEX idx_permanent_ban_payout_user (user_id, created_at),
    INDEX idx_permanent_ban_payout_batch (batch_no, id)
);

INSERT INTO admin_permissions(
    code,name,module_name,action_name,sort_order,system_permission
) VALUES
('PERMANENT_BAN_PAYOUT_VIEW','查看永久封禁余额处理','永久封禁余额处理','查看',440,TRUE),
('PERMANENT_BAN_PAYOUT_EXPORT','导出永久封禁余额','永久封禁余额处理','导出',450,TRUE),
('PERMANENT_BAN_PAYOUT_PROCESS','处理永久封禁余额','永久封禁余额处理','处理',460,TRUE);

INSERT IGNORE INTO admin_role_permissions(role_id,permission_id)
SELECT role.id,permission.id
FROM admin_roles role
JOIN admin_permissions permission
WHERE permission.code IN (
    'PERMANENT_BAN_PAYOUT_VIEW',
    'PERMANENT_BAN_PAYOUT_EXPORT',
    'PERMANENT_BAN_PAYOUT_PROCESS'
)
AND role.code IN ('SUPER_ADMIN','GENERAL_ADMIN_L1','FINANCE_ADMIN');

INSERT IGNORE INTO admin_role_permissions(role_id,permission_id)
SELECT role.id,permission.id
FROM admin_roles role
JOIN admin_permissions permission
WHERE permission.code='PERMANENT_BAN_PAYOUT_VIEW'
AND role.code='GENERAL_ADMIN_L3';

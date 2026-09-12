-- App 尚未上线，删除已经退出的回答者结束纠纷和质量退款复核业务。
DROP TABLE IF EXISTS inquiry_quality_reviews;
DROP TABLE IF EXISTS inquiry_end_disputes;
DROP TABLE IF EXISTS inquiry_dispute_events;

ALTER TABLE inquiries
    DROP INDEX idx_inquiry_end_reminder,
    DROP COLUMN confirmation_deadline,
    DROP COLUMN end_requested_at,
    DROP COLUMN end_reminder_stage;

DELETE role_permission
FROM admin_role_permissions role_permission
JOIN admin_permissions permission ON permission.id = role_permission.permission_id
WHERE permission.code IN (
    'INQUIRY_DISPUTE_VIEW',
    'INQUIRY_DISPUTE_PROCESS',
    'ANSWER_QUALITY_REVIEW',
    'ANSWER_QUALITY_EVIDENCE',
    'ANSWER_QUALITY_PENALIZE'
);

DELETE FROM admin_permissions
WHERE code IN (
    'INQUIRY_DISPUTE_VIEW',
    'INQUIRY_DISPUTE_PROCESS',
    'ANSWER_QUALITY_REVIEW',
    'ANSWER_QUALITY_EVIDENCE',
    'ANSWER_QUALITY_PENALIZE'
);

UPDATE admin_permissions
SET name = '查看交流评价',
    module_name = '交流评价',
    action_name = '查看',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'ANSWER_QUALITY_VIEW';

UPDATE admin_permissions
SET name = CASE code
        WHEN 'RISK_WATCH_VIEW' THEN '查看用户风险'
        WHEN 'RISK_WATCH_PROCESS' THEN '处理用户风险'
        ELSE name
    END,
    module_name = '用户风险',
    action_name = CASE code
        WHEN 'RISK_WATCH_VIEW' THEN '查看'
        WHEN 'RISK_WATCH_PROCESS' THEN '处理'
        ELSE action_name
    END,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code IN ('RISK_WATCH_VIEW', 'RISK_WATCH_PROCESS');

-- V115 移除了预约/购买时长模型。把升级前尚未处理的旧请求并入当前直呼超时流程，
-- 由服务按现有退款账务逻辑立即解冻，避免旧状态永久占用资金。
UPDATE inquiry_voice_calls
SET status = 'CONNECTING',
    connect_deadline = CURRENT_TIMESTAMP(6),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE status IN ('PENDING', 'ACCEPTED');

-- 普通询问只保留直接语音通话：按实际接通秒数计费，不再维护购买时长、预约或付费时段状态。

UPDATE inquiries
SET status = 'ACTIVE'
WHERE status IN ('PAID_ACTIVE', 'TEXT_ENDED');

ALTER TABLE inquiries
    DROP INDEX idx_inquiry_paid_session_expiry,
    DROP CHECK chk_inquiry_purchased_minutes,
    DROP COLUMN session_type,
    DROP COLUMN purchased_minutes,
    DROP COLUMN paid_session_started_at,
    DROP COLUMN paid_session_ends_at;

DROP TABLE IF EXISTS inquiry_audio_extensions;

ALTER TABLE inquiry_voice_signals
    DROP FOREIGN KEY fk_voice_signal_appointment;

ALTER TABLE voice_call_events
    DROP FOREIGN KEY fk_voice_call_events_appointment;

RENAME TABLE inquiry_audio_appointments TO inquiry_voice_calls;

-- 原有时间复合索引同时被 MySQL 用作用户外键索引；先补充稳定的单列外键索引再移除。
ALTER TABLE inquiry_voice_calls
    ADD INDEX idx_voice_call_answerer (answerer_id),
    ADD INDEX idx_voice_call_questioner (questioner_id);

ALTER TABLE inquiry_voice_calls
    DROP INDEX idx_audio_appointment_answerer_time,
    DROP INDEX idx_audio_appointment_questioner_time,
    DROP INDEX idx_audio_appointment_timeout,
    DROP INDEX idx_audio_appointment_start,
    DROP INDEX idx_audio_appointment_end,
    DROP INDEX idx_audio_appointment_type_attempts,
    CHANGE COLUMN scheduled_end_at max_end_at DATETIME(6) NOT NULL,
    CHANGE COLUMN amount reserved_amount DECIMAL(14, 2) NOT NULL,
    DROP COLUMN appointment_type,
    DROP COLUMN scheduled_start_at,
    DROP COLUMN duration_minutes,
    DROP COLUMN previous_inquiry_status,
    DROP COLUMN five_minute_warning_sent,
    DROP COLUMN response_deadline,
    DROP COLUMN attempt_number,
    DROP COLUMN no_show_party,
    DROP COLUMN started_at,
    RENAME INDEX idx_audio_appointment_inquiry TO idx_voice_call_inquiry,
    ADD INDEX idx_voice_call_connect_timeout (status, connect_deadline),
    ADD INDEX idx_voice_call_max_end (status, max_end_at),
    ADD INDEX idx_voice_call_reconnect_timeout (status, reconnect_deadline);

ALTER TABLE inquiry_voice_signals
    CHANGE COLUMN appointment_id voice_call_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_voice_signal_call
        FOREIGN KEY (voice_call_id) REFERENCES inquiry_voice_calls(id),
    RENAME INDEX idx_voice_signal_recipient TO idx_voice_signal_recipient_call;

ALTER TABLE voice_call_events
    CHANGE COLUMN appointment_id voice_call_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_voice_call_events_call
        FOREIGN KEY (voice_call_id) REFERENCES inquiry_voice_calls(id),
    RENAME INDEX idx_voice_call_events_appointment TO idx_voice_call_events_call;

UPDATE wallet_transactions
SET transaction_type = CASE transaction_type
        WHEN 'AUDIO_APPOINTMENT_FREEZE' THEN 'VOICE_CALL_FREEZE'
        WHEN 'AUDIO_APPOINTMENT_REFUND' THEN 'VOICE_CALL_REFUND'
        ELSE transaction_type
    END,
    reference_type = CASE reference_type
        WHEN 'AUDIO_APPOINTMENT' THEN 'VOICE_CALL'
        ELSE reference_type
    END
WHERE transaction_type IN ('AUDIO_APPOINTMENT_FREEZE', 'AUDIO_APPOINTMENT_REFUND')
   OR reference_type = 'AUDIO_APPOINTMENT';

UPDATE wallet_income_holds
SET reference_type = 'VOICE_CALL'
WHERE reference_type = 'AUDIO_APPOINTMENT';

UPDATE platform_fee_records
SET reference_type = 'VOICE_CALL'
WHERE reference_type = 'AUDIO_APPOINTMENT';

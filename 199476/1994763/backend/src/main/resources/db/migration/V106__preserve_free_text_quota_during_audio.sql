ALTER TABLE inquiry_messages
    ADD COLUMN counts_toward_free_limit BOOLEAN NOT NULL DEFAULT TRUE AFTER message_type,
    ADD INDEX idx_inquiry_message_free_quota (
        inquiry_id,
        sender_id,
        counts_toward_free_limit,
        message_type
    );

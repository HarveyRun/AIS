ALTER TABLE inquiries
    ADD COLUMN last_message_at DATETIME(6) NULL AFTER ended_at;

UPDATE inquiries i
SET i.last_message_at = (
    SELECT MAX(m.created_at)
    FROM inquiry_messages m
    WHERE m.inquiry_id = i.id
);

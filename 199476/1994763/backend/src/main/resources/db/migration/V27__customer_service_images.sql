ALTER TABLE customer_service_messages
    ADD COLUMN message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' AFTER sender_type,
    ADD COLUMN attachment_url VARCHAR(500) NULL AFTER content,
    ADD COLUMN attachment_name VARCHAR(255) NULL AFTER attachment_url,
    ADD COLUMN attachment_size BIGINT NULL AFTER attachment_name;

ALTER TABLE inquiry_messages
    ADD COLUMN attachment_key VARCHAR(500) NULL AFTER attachment_url;

ALTER TABLE customer_service_messages
    ADD COLUMN attachment_key VARCHAR(500) NULL AFTER attachment_url;

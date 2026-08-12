ALTER TABLE inquiries
    ADD COLUMN questioner_unread_count INT NOT NULL DEFAULT 0 AFTER funds_status,
    ADD COLUMN answerer_unread_count INT NOT NULL DEFAULT 0 AFTER questioner_unread_count;

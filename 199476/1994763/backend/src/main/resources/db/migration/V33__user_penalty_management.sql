ALTER TABLE users
    ADD COLUMN ban_reason VARCHAR(300) NULL AFTER account_status,
    ADD COLUMN banned_at DATETIME(6) NULL AFTER ban_reason,
    ADD COLUMN ban_until DATETIME(6) NULL AFTER banned_at,
    ADD COLUMN banned_by_admin_id BIGINT NULL AFTER ban_until,
    ADD CONSTRAINT fk_user_banned_by_admin
        FOREIGN KEY (banned_by_admin_id) REFERENCES admin_users(id),
    ADD INDEX idx_user_ban_expiry (account_status, ban_until);

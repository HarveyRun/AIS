ALTER TABLE users
    ADD COLUMN register_ip VARCHAR(45) NULL AFTER account_status,
    ADD COLUMN register_location VARCHAR(100) NULL AFTER register_ip,
    ADD COLUMN last_login_ip VARCHAR(45) NULL AFTER register_location,
    ADD COLUMN last_login_location VARCHAR(100) NULL AFTER last_login_ip,
    ADD COLUMN last_login_at DATETIME(6) NULL AFTER last_login_location;

CREATE TABLE user_login_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    ip_location VARCHAR(100) NOT NULL,
    logged_in_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_login_record_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_login_record_user_time (user_id, logged_in_at)
);

ALTER TABLE inquiries
    ADD COLUMN request_ip VARCHAR(45) NULL AFTER question,
    ADD COLUMN request_location VARCHAR(100) NULL AFTER request_ip;

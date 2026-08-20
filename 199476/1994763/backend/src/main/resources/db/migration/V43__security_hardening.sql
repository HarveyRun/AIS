ALTER TABLE users
    ADD COLUMN account_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER account_status;

ALTER TABLE verification_codes
    ADD COLUMN purpose VARCHAR(30) NOT NULL DEFAULT 'LOGIN' AFTER phone,
    ADD COLUMN request_device_id VARCHAR(100) NOT NULL DEFAULT 'unknown' AFTER request_ip,
    DROP INDEX idx_verification_phone_time,
    DROP INDEX idx_verification_active,
    ADD INDEX idx_verification_phone_purpose_time (phone, purpose, created_at),
    ADD INDEX idx_verification_device_time (request_device_id, created_at),
    ADD INDEX idx_verification_active (phone, purpose, consumed_at, expires_at);

ALTER TABLE user_login_records
    ADD COLUMN device_id VARCHAR(100) NOT NULL DEFAULT 'unknown' AFTER ip_location,
    ADD INDEX idx_user_login_device_time (device_id, logged_in_at);

ALTER TABLE admin_sessions
    ADD COLUMN login_ip VARCHAR(45) NOT NULL DEFAULT 'unknown' AFTER token_hash,
    ADD COLUMN device_id VARCHAR(100) NOT NULL DEFAULT 'unknown' AFTER login_ip;

CREATE TABLE login_attempts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subject_type VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    request_ip VARCHAR(45) NOT NULL,
    device_id VARCHAR(100) NOT NULL,
    successful BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_login_attempt_phone (subject_type, phone, successful, created_at),
    INDEX idx_login_attempt_ip (subject_type, request_ip, successful, created_at),
    INDEX idx_login_attempt_device (subject_type, device_id, successful, created_at)
);

CREATE TABLE security_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    admin_user_id BIGINT NULL,
    event_type VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    request_ip VARCHAR(45) NULL,
    device_id VARCHAR(100) NULL,
    detail VARCHAR(1000) NULL,
    review_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_security_event_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_security_event_admin FOREIGN KEY (admin_user_id) REFERENCES admin_users(id),
    CONSTRAINT fk_security_event_reviewer FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_security_event_status_time (review_status, created_at),
    INDEX idx_security_event_severity_time (severity, created_at),
    INDEX idx_security_event_user_time (user_id, created_at)
);

ALTER TABLE wallet_accounts
    ADD COLUMN recharge_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER frozen_balance,
    ADD COLUMN income_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER recharge_balance,
    ADD COLUMN pending_income_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER income_balance,
    ADD COLUMN frozen_recharge_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER pending_income_balance,
    ADD COLUMN frozen_income_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER frozen_recharge_balance;

UPDATE wallet_accounts
SET recharge_balance = available_balance,
    frozen_recharge_balance = frozen_balance;

ALTER TABLE wallet_accounts
    ADD CONSTRAINT chk_wallet_recharge_non_negative CHECK (recharge_balance >= 0.00),
    ADD CONSTRAINT chk_wallet_income_non_negative CHECK (income_balance >= 0.00),
    ADD CONSTRAINT chk_wallet_pending_income_non_negative CHECK (pending_income_balance >= 0.00),
    ADD CONSTRAINT chk_wallet_frozen_recharge_non_negative CHECK (frozen_recharge_balance >= 0.00),
    ADD CONSTRAINT chk_wallet_frozen_income_non_negative CHECK (frozen_income_balance >= 0.00),
    ADD CONSTRAINT chk_wallet_available_sources CHECK (available_balance = recharge_balance + income_balance),
    ADD CONSTRAINT chk_wallet_frozen_sources CHECK (frozen_balance = frozen_recharge_balance + frozen_income_balance);

ALTER TABLE inquiries
    ADD COLUMN frozen_recharge_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER amount,
    ADD COLUMN frozen_income_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER frozen_recharge_amount,
    ADD CONSTRAINT chk_inquiry_frozen_sources CHECK (
        frozen_recharge_amount >= 0.00
        AND frozen_income_amount >= 0.00
        AND frozen_recharge_amount + frozen_income_amount <= amount
    );

UPDATE inquiries
SET frozen_recharge_amount = amount
WHERE funds_status = 'FROZEN';

CREATE TABLE wallet_income_holds (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    inquiry_id BIGINT NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    release_at DATETIME(6) NOT NULL,
    released_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_wallet_income_hold_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_wallet_income_hold_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT uq_wallet_income_hold_inquiry UNIQUE (inquiry_id),
    CONSTRAINT chk_wallet_income_hold_amount CHECK (amount > 0.00),
    INDEX idx_wallet_income_hold_release (status, release_at)
);

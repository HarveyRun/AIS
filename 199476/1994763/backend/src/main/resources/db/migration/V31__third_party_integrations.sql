CREATE TABLE verification_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) NOT NULL,
    code_hash CHAR(64) NOT NULL,
    request_ip VARCHAR(64) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_verification_phone_time (phone, created_at),
    INDEX idx_verification_ip_time (request_ip, created_at),
    INDEX idx_verification_active (phone, consumed_at, expires_at)
);

ALTER TABLE recharges
    ADD COLUMN provider_trade_no VARCHAR(100) NULL AFTER order_no;

ALTER TABLE certification_materials
    ADD COLUMN public_url VARCHAR(1000) NULL AFTER storage_key;

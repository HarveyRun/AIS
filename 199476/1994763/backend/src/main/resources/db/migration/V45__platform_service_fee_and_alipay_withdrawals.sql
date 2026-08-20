CREATE TABLE platform_fee_settings (
    id BIGINT PRIMARY KEY,
    service_fee_rate DECIMAL(7, 6) NOT NULL,
    updated_by_admin_id BIGINT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_platform_service_fee_rate
        CHECK (service_fee_rate >= 0.000000 AND service_fee_rate <= 0.990000),
    CONSTRAINT fk_platform_fee_updated_admin
        FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(id)
);

INSERT INTO platform_fee_settings (id, service_fee_rate)
VALUES (1, 0.050000);

ALTER TABLE inquiries
    ADD COLUMN service_fee_rate DECIMAL(7, 6) NOT NULL DEFAULT 0.000000 AFTER amount,
    ADD COLUMN service_fee_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER service_fee_rate,
    ADD COLUMN answerer_income_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER service_fee_amount;

UPDATE inquiries
SET answerer_income_amount = amount
WHERE answerer_income_amount = 0.00;

ALTER TABLE inquiries
    ADD CONSTRAINT chk_inquiry_service_fee_rate
        CHECK (service_fee_rate >= 0.000000 AND service_fee_rate <= 0.990000),
    ADD CONSTRAINT chk_inquiry_service_fee_amount
        CHECK (service_fee_amount >= 0.00 AND service_fee_amount <= amount),
    ADD CONSTRAINT chk_inquiry_answerer_income
        CHECK (answerer_income_amount >= 0.00 AND answerer_income_amount + service_fee_amount = amount);

CREATE TABLE platform_fee_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL UNIQUE,
    gross_amount DECIMAL(14, 2) NOT NULL,
    service_fee_rate DECIMAL(7, 6) NOT NULL,
    service_fee_amount DECIMAL(14, 2) NOT NULL,
    answerer_income_amount DECIMAL(14, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_platform_fee_record_inquiry
        FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT chk_platform_fee_record_amounts
        CHECK (
            gross_amount > 0.00
            AND service_fee_amount >= 0.00
            AND answerer_income_amount >= 0.00
            AND service_fee_amount + answerer_income_amount = gross_amount
        )
);

CREATE TABLE alipay_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    real_name VARCHAR(80) NOT NULL,
    account_ciphertext VARCHAR(512) NOT NULL,
    account_masked VARCHAR(120) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_alipay_account_user FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE withdrawals
    DROP FOREIGN KEY fk_withdrawal_bank_card,
    MODIFY bank_card_id BIGINT NULL,
    MODIFY bank_name_snapshot VARCHAR(100) NULL,
    MODIFY card_last_four_snapshot VARCHAR(4) NULL,
    ADD COLUMN alipay_account_id BIGINT NULL AFTER bank_card_id,
    ADD COLUMN payee_name_snapshot VARCHAR(80) NULL AFTER arrival_amount,
    ADD COLUMN alipay_account_ciphertext_snapshot VARCHAR(512) NULL AFTER payee_name_snapshot,
    ADD COLUMN alipay_account_masked_snapshot VARCHAR(120) NULL AFTER alipay_account_ciphertext_snapshot,
    ADD COLUMN batch_no VARCHAR(64) NULL AFTER status,
    ADD COLUMN exported_at DATETIME(6) NULL AFTER batch_no,
    ADD CONSTRAINT fk_withdrawal_alipay_account
        FOREIGN KEY (alipay_account_id) REFERENCES alipay_accounts(id),
    ADD INDEX idx_withdrawal_export (status, batch_no, id);

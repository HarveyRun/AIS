ALTER TABLE alipay_accounts
    MODIFY real_name VARCHAR(80) NULL,
    ADD COLUMN authorization_type VARCHAR(20) NOT NULL DEFAULT 'LEGACY_MANUAL' AFTER user_id,
    ADD COLUMN identifier_type VARCHAR(20) NULL AFTER authorization_type,
    ADD COLUMN alipay_user_id_hash VARCHAR(64) NULL AFTER account_ciphertext,
    ADD COLUMN display_name VARCHAR(120) NULL AFTER account_masked,
    ADD COLUMN authorized_at DATETIME(6) NULL AFTER display_name,
    ADD UNIQUE INDEX uk_alipay_account_user_id_hash (alipay_user_id_hash);

UPDATE alipay_accounts
SET authorization_type = 'LEGACY_MANUAL'
WHERE authorization_type IS NULL OR authorization_type = '';

ALTER TABLE withdrawals
    ADD COLUMN alipay_identifier_type_snapshot VARCHAR(20) NULL
        AFTER payee_name_snapshot;

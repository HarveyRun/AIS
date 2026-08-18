DELETE FROM app_test_login_accounts
WHERE id = 1
  AND phone IS NULL;

ALTER TABLE app_test_login_accounts
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT,
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE AFTER enabled,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER deleted;

CREATE INDEX idx_app_test_login_accounts_available
    ON app_test_login_accounts (deleted, enabled, updated_at);

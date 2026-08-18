CREATE TABLE app_test_login_accounts (
    id BIGINT PRIMARY KEY,
    phone VARCHAR(20) NULL UNIQUE,
    verification_code CHAR(4) NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by_admin_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_app_test_login_account_admin
        FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(id) ON DELETE SET NULL
);

INSERT INTO app_test_login_accounts (id, enabled)
VALUES (1, FALSE);

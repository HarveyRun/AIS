CREATE TABLE app_global_settings (
    id TINYINT NOT NULL,
    non_labor_income_fee_rate DECIMAL(7, 6) NOT NULL DEFAULT 0.500000,
    first_experience_reward_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    first_experience_reward_amount DECIMAL(14, 2) NOT NULL DEFAULT 3.00,
    first_experience_reward_start_at DATETIME(6) NULL,
    first_experience_reward_end_at DATETIME(6) NULL,
    inquiry_deposit_amount DECIMAL(14, 2) NOT NULL DEFAULT 2.00,
    free_pending_inquiry_limit INT NOT NULL DEFAULT 3,
    income_hold_hours INT NOT NULL DEFAULT 24,
    withdrawal_min_amount DECIMAL(14, 2) NOT NULL DEFAULT 1.00,
    withdrawal_max_amount DECIMAL(14, 2) NOT NULL DEFAULT 9999.00,
    withdrawal_daily_limit DECIMAL(14, 2) NOT NULL DEFAULT 20000.00,
    payout_account_cooldown_hours INT NOT NULL DEFAULT 24,
    tip_preset_amounts VARCHAR(100) NOT NULL DEFAULT '1,3,5,18,68',
    tip_max_amount INT NOT NULL DEFAULT 5000,
    initial_text_message_limit INT NOT NULL DEFAULT 50,
    text_message_max_length INT NOT NULL DEFAULT 100,
    voice_reward_seconds INT NOT NULL DEFAULT 300,
    voice_reward_messages INT NOT NULL DEFAULT 50,
    voice_ring_timeout_seconds INT NOT NULL DEFAULT 60,
    voice_reconnect_timeout_seconds INT NOT NULL DEFAULT 60,
    inquiry_response_timeout_hours INT NOT NULL DEFAULT 72,
    inquiry_max_duration_days INT NOT NULL DEFAULT 20,
    hourly_rate_min INT NOT NULL DEFAULT 1,
    hourly_rate_max INT NOT NULL DEFAULT 5000,
    max_unapproved_experiences INT NOT NULL DEFAULT 3,
    experience_title_max_length INT NOT NULL DEFAULT 18,
    experience_description_max_length INT NOT NULL DEFAULT 400,
    proof_archive_max_bytes BIGINT NOT NULL DEFAULT 2147483648,
    home_page_size INT NOT NULL DEFAULT 20,
    experience_publish_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    inquiry_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    voice_call_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    experience_tip_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    recharge_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    withdrawal_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by_admin_id BIGINT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_app_global_settings_singleton CHECK (id = 1),
    CONSTRAINT fk_app_global_settings_admin FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(id)
);

INSERT INTO app_global_settings(id) VALUES (1);

INSERT INTO admin_permissions(code,name,module_name,action_name,sort_order,system_permission) VALUES
('APP_GLOBAL_SETTING_VIEW','查看App全局设置','App全局设置','查看',1140,TRUE),
('APP_GLOBAL_SETTING_EDIT','修改App全局设置','App全局设置','修改',1150,TRUE);

INSERT IGNORE INTO admin_role_permissions(role_id,permission_id)
SELECT role.id,permission.id
FROM admin_roles role
JOIN admin_permissions permission
  ON permission.code IN ('APP_GLOBAL_SETTING_VIEW','APP_GLOBAL_SETTING_EDIT')
WHERE role.code IN ('SUPER_ADMIN','GENERAL_ADMIN_L1');

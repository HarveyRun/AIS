ALTER TABLE platform_fee_settings
    ADD COLUMN client_platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID' AFTER id,
    ADD UNIQUE INDEX uk_platform_fee_setting_platform (client_platform);

UPDATE platform_fee_settings
SET client_platform = 'ANDROID'
WHERE id = 1;

INSERT INTO platform_fee_settings (id, client_platform, service_fee_rate, updated_by_admin_id)
SELECT 2, 'IOS', service_fee_rate, updated_by_admin_id
FROM platform_fee_settings
WHERE id = 1;

ALTER TABLE inquiries
    ADD COLUMN client_platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID' AFTER amount;

ALTER TABLE platform_fee_records
    ADD COLUMN client_platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID' AFTER inquiry_id;

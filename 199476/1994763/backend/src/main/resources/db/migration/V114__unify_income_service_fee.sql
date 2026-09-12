ALTER TABLE certifications
    ADD COLUMN source_client_platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID' AFTER experience_job;

ALTER TABLE app_global_settings
    DROP COLUMN non_labor_income_fee_rate;

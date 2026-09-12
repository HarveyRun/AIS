ALTER TABLE app_global_settings
    ADD COLUMN curated_membership_price DECIMAL(10,2) NOT NULL DEFAULT 99.00
        AFTER curated_membership_months,
    ADD CONSTRAINT chk_curated_membership_price
        CHECK (curated_membership_price BETWEEN 1.00 AND 9999.00);

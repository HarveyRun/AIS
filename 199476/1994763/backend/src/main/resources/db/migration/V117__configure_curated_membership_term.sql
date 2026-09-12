ALTER TABLE app_global_settings
    ADD COLUMN curated_membership_months INT NOT NULL DEFAULT 1 AFTER home_page_size,
    ADD CONSTRAINT chk_curated_membership_months
        CHECK (curated_membership_months BETWEEN 1 AND 1200);

ALTER TABLE curated_membership_orders
    ADD COLUMN duration_months INT NOT NULL DEFAULT 1 AFTER amount,
    ADD CONSTRAINT chk_curated_order_duration_months
        CHECK (duration_months BETWEEN 1 AND 1200);

ALTER TABLE curated_memberships
    ADD COLUMN duration_months INT NOT NULL DEFAULT 1 AFTER expires_at,
    ADD CONSTRAINT chk_curated_membership_duration_months
        CHECK (duration_months BETWEEN 1 AND 1200);

ALTER TABLE home_banners
    ADD COLUMN start_at DATETIME(6) NULL AFTER sort_order,
    ADD COLUMN end_at DATETIME(6) NULL AFTER start_at;

UPDATE home_banners
SET start_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    end_at = '2099-12-31 23:59:59.999999'
WHERE start_at IS NULL
   OR end_at IS NULL;

ALTER TABLE home_banners
    MODIFY COLUMN start_at DATETIME(6) NOT NULL,
    MODIFY COLUMN end_at DATETIME(6) NOT NULL;

CREATE INDEX idx_home_banners_public_schedule
    ON home_banners(deleted, enabled, start_at, end_at, sort_order, id);

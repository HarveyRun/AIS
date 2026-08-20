ALTER TABLE users
    ALTER COLUMN inquiry_price_max SET DEFAULT 5000,
    ADD COLUMN inquiry_price_updated_at DATETIME(6) NULL AFTER inquiry_price_max,
    ADD COLUMN accepting_inquiries_updated_at DATETIME(6) NULL AFTER accepting_inquiries;

UPDATE users
SET inquiry_price_max = 5000
WHERE inquiry_price_min = 1
  AND inquiry_price_max = 999;

UPDATE users
SET accepting_inquiries = FALSE;

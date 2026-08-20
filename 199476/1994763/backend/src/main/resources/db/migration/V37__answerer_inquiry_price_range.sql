ALTER TABLE users
    ADD COLUMN inquiry_price_min INT NOT NULL DEFAULT 1 AFTER accepting_inquiries,
    ADD COLUMN inquiry_price_max INT NOT NULL DEFAULT 999 AFTER inquiry_price_min;


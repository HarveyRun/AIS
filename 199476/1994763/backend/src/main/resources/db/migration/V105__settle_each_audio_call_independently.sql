ALTER TABLE wallet_income_holds
    ADD INDEX idx_wallet_income_hold_inquiry (inquiry_id, status);

ALTER TABLE wallet_income_holds
    DROP INDEX uq_wallet_income_hold_inquiry,
    ADD COLUMN reference_type VARCHAR(40) NOT NULL DEFAULT 'INQUIRY' AFTER inquiry_id,
    ADD COLUMN reference_id BIGINT NULL AFTER reference_type;

UPDATE wallet_income_holds
SET reference_id = inquiry_id
WHERE reference_id IS NULL;

ALTER TABLE wallet_income_holds
    MODIFY reference_id BIGINT NOT NULL,
    ADD CONSTRAINT uq_wallet_income_hold_reference UNIQUE (reference_type, reference_id);

ALTER TABLE platform_fee_records
    ADD INDEX idx_platform_fee_inquiry (inquiry_id, status);

ALTER TABLE platform_fee_records
    DROP INDEX inquiry_id,
    ADD COLUMN reference_type VARCHAR(40) NOT NULL DEFAULT 'INQUIRY' AFTER inquiry_id,
    ADD COLUMN reference_id BIGINT NULL AFTER reference_type;

UPDATE platform_fee_records
SET reference_id = inquiry_id
WHERE reference_id IS NULL;

ALTER TABLE platform_fee_records
    MODIFY reference_id BIGINT NOT NULL,
    ADD CONSTRAINT uq_platform_fee_reference UNIQUE (reference_type, reference_id);

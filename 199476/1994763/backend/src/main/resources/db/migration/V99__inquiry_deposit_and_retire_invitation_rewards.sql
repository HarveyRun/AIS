ALTER TABLE inquiries
    ADD COLUMN deposit_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER amount,
    ADD COLUMN deposit_frozen_recharge_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER deposit_amount,
    ADD COLUMN deposit_frozen_income_amount DECIMAL(14, 2) NOT NULL DEFAULT 0.00 AFTER deposit_frozen_recharge_amount,
    ADD COLUMN deposit_status VARCHAR(20) NOT NULL DEFAULT 'NONE' AFTER deposit_frozen_income_amount;

UPDATE home_banners
SET enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE deleted = FALSE
  AND action_type IN (
      'INVITE_EXPERIENCE',
      'INVITE_PUBLIC_EXPERIENCE',
      'INVITE_MONETIZED_EXPERIENCE'
  );

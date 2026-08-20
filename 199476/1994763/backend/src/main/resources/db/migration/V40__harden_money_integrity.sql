ALTER TABLE wallet_accounts
    ADD CONSTRAINT chk_wallet_available_non_negative CHECK (available_balance >= 0.00),
    ADD CONSTRAINT chk_wallet_frozen_non_negative CHECK (frozen_balance >= 0.00),
    ADD CONSTRAINT chk_wallet_withdrawn_non_negative CHECK (total_withdrawn >= 0.00);

ALTER TABLE inquiries
    ADD CONSTRAINT chk_inquiry_amount_positive_integer
        CHECK (amount >= 1.00 AND amount = FLOOR(amount));

ALTER TABLE wallet_transactions
    ADD CONSTRAINT chk_wallet_transaction_amount_positive CHECK (amount > 0.00),
    ADD CONSTRAINT chk_wallet_transaction_available_non_negative CHECK (available_after >= 0.00),
    ADD CONSTRAINT chk_wallet_transaction_frozen_non_negative CHECK (frozen_after >= 0.00),
    ADD INDEX idx_wallet_transaction_operation
        (user_id, transaction_type, reference_type, reference_id);

ALTER TABLE recharges
    ADD CONSTRAINT chk_recharge_amount_positive_integer
        CHECK (amount >= 1.00 AND amount = FLOOR(amount)),
    ADD CONSTRAINT uq_recharge_provider_trade_no UNIQUE (provider_trade_no);

ALTER TABLE withdrawals
    ADD CONSTRAINT chk_withdrawal_amount_positive_integer
        CHECK (amount >= 1.00 AND amount = FLOOR(amount)),
    ADD CONSTRAINT chk_withdrawal_fee_valid CHECK (fee >= 0.00 AND fee <= amount),
    ADD CONSTRAINT chk_withdrawal_arrival_valid
        CHECK (arrival_amount >= 0.00 AND arrival_amount + fee = amount);

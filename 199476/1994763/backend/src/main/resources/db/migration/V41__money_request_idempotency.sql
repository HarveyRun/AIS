ALTER TABLE recharges
    ADD COLUMN request_no VARCHAR(64) NULL AFTER order_no;

UPDATE recharges
SET request_no = CONCAT('legacy-recharge-', id)
WHERE request_no IS NULL;

ALTER TABLE recharges
    MODIFY request_no VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uq_recharge_user_request UNIQUE (user_id, request_no);

ALTER TABLE withdrawals
    ADD COLUMN request_no VARCHAR(64) NULL AFTER bank_card_id;

UPDATE withdrawals
SET request_no = CONCAT('legacy-withdrawal-', id)
WHERE request_no IS NULL;

ALTER TABLE withdrawals
    MODIFY request_no VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uq_withdrawal_user_request UNIQUE (user_id, request_no);

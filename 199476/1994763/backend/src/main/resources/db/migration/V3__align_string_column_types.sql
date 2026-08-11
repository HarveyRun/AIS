ALTER TABLE auth_sessions MODIFY token_hash VARCHAR(64) NOT NULL;
ALTER TABLE bank_cards MODIFY card_last_four VARCHAR(4) NOT NULL;
ALTER TABLE withdrawals MODIFY card_last_four_snapshot VARCHAR(4) NOT NULL;

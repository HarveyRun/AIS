SET @drop_service_fee_check = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_schema = DATABASE()
              AND table_name = 'inquiries'
              AND constraint_name = 'chk_inquiry_service_fee_amount'
        ),
        'ALTER TABLE inquiries DROP CHECK chk_inquiry_service_fee_amount',
        'SELECT 1'
    )
);
PREPARE drop_service_fee_check_statement FROM @drop_service_fee_check;
EXECUTE drop_service_fee_check_statement;
DEALLOCATE PREPARE drop_service_fee_check_statement;

SET @drop_answerer_income_check = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.table_constraints
            WHERE constraint_schema = DATABASE()
              AND table_name = 'inquiries'
              AND constraint_name = 'chk_inquiry_answerer_income'
        ),
        'ALTER TABLE inquiries DROP CHECK chk_inquiry_answerer_income',
        'SELECT 1'
    )
);
PREPARE drop_answerer_income_check_statement FROM @drop_answerer_income_check;
EXECUTE drop_answerer_income_check_statement;
DEALLOCATE PREPARE drop_answerer_income_check_statement;

UPDATE inquiries
SET service_fee_amount = ROUND(settleable_amount * service_fee_rate, 2),
    answerer_income_amount = settleable_amount - ROUND(settleable_amount * service_fee_rate, 2);

ALTER TABLE inquiries
    ADD CONSTRAINT chk_inquiry_service_fee_amount
        CHECK (
            service_fee_amount >= 0.00
            AND service_fee_amount <= settleable_amount
        ),
    ADD CONSTRAINT chk_inquiry_answerer_income
        CHECK (
            answerer_income_amount >= 0.00
            AND answerer_income_amount + service_fee_amount = settleable_amount
        );

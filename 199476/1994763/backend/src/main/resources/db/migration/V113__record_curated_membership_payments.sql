INSERT INTO wallet_transactions (
    user_id,
    transaction_type,
    direction,
    amount,
    available_after,
    frozen_after,
    reference_type,
    reference_id,
    description,
    created_at
)
SELECT
    membership_order.user_id,
    'CURATED_MEMBERSHIP_PURCHASE',
    'OUT',
    membership_order.amount,
    wallet.available_balance,
    wallet.frozen_balance,
    'CURATED_MEMBERSHIP',
    membership_order.id,
    CASE
        WHEN membership_order.channel = 'TEST' THEN '测试开通严选直聊'
        ELSE '开通严选直聊'
    END,
    COALESCE(membership_order.paid_at, membership_order.created_at)
FROM curated_membership_orders membership_order
JOIN wallet_accounts wallet ON wallet.user_id = membership_order.user_id
WHERE membership_order.status = 'PAID'
  AND NOT EXISTS (
      SELECT 1
      FROM wallet_transactions existing_transaction
      WHERE existing_transaction.user_id = membership_order.user_id
        AND existing_transaction.transaction_type = 'CURATED_MEMBERSHIP_PURCHASE'
        AND existing_transaction.reference_type = 'CURATED_MEMBERSHIP'
        AND existing_transaction.reference_id = membership_order.id
  );

INSERT IGNORE INTO fund_vouchers (
    voucher_no,
    business_type,
    business_id,
    action_code,
    description,
    occurred_at
)
SELECT
    CONCAT('FV-CURATED-MEMBERSHIP-', membership_order.id),
    'CURATED_MEMBERSHIP',
    CAST(membership_order.id AS CHAR),
    'PAID',
    CASE
        WHEN membership_order.channel = 'TEST' THEN '测试开通严选直聊'
        ELSE '严选直聊开通到账'
    END,
    COALESCE(membership_order.paid_at, membership_order.created_at)
FROM curated_membership_orders membership_order
WHERE membership_order.status = 'PAID';

INSERT INTO fund_entries (voucher_id, account_code, user_id, signed_amount)
SELECT
    voucher.id,
    CASE
        WHEN entry_slot.slot = 1 AND membership_order.channel = 'TEST' THEN 'TEST_CLEARING'
        WHEN entry_slot.slot = 1 THEN 'ALIPAY_CLEARING'
        WHEN membership_order.channel = 'TEST' THEN 'TEST_MEMBERSHIP_REVENUE'
        ELSE 'PLATFORM_MEMBERSHIP_REVENUE'
    END,
    NULL,
    CASE WHEN entry_slot.slot = 1 THEN membership_order.amount ELSE -membership_order.amount END
FROM curated_membership_orders membership_order
JOIN fund_vouchers voucher
  ON voucher.business_type = 'CURATED_MEMBERSHIP'
 AND voucher.business_id = CAST(membership_order.id AS CHAR)
 AND voucher.action_code = 'PAID'
CROSS JOIN (
    SELECT 1 AS slot
    UNION ALL
    SELECT 2 AS slot
) entry_slot
WHERE membership_order.status = 'PAID'
  AND NOT EXISTS (
      SELECT 1 FROM fund_entries existing_entry WHERE existing_entry.voucher_id = voucher.id
  );

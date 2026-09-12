-- Reset all user-generated and transactional data while preserving the schema
-- and platform configuration required to operate and test the application.
--
-- Preserved:
--   admin_users / admin_roles / admin_permissions and their relationships
--   admin_system_state
--   app_test_login_accounts
--   analytics_event_definitions
--   app_versions
--   home_banners
--   platform_fee_settings
--   system_announcements
--   flyway_schema_history

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM system_announcement_reads;
DELETE FROM notifications;

DELETE FROM curated_voice_signals;
DELETE FROM curated_voice_calls;
DELETE FROM curated_conversation_reads;
DELETE FROM curated_messages;
DELETE FROM curated_chat_blocks;
DELETE FROM curated_conversations;
DELETE FROM curated_membership_orders;
DELETE FROM curated_memberships;
DELETE FROM curated_membership_materials;
DELETE FROM curated_membership_applications;

DELETE FROM user_communication_blocks;
DELETE FROM inquiry_dispute_events;
DELETE FROM inquiry_end_disputes;
DELETE FROM inquiry_evaluations;
DELETE FROM inquiry_quality_reviews;
DELETE FROM inquiry_timeout_refunds;
DELETE FROM user_inquiry_violation_summaries;
DELETE FROM inquiry_voice_signals;
DELETE FROM inquiry_audio_extensions;
DELETE FROM inquiry_audio_appointments;
DELETE FROM inquiry_messages;
DELETE FROM platform_fee_records;
DELETE FROM wallet_income_holds;
DELETE FROM inquiries;

DELETE FROM experience_tips;
DELETE FROM first_experience_rewards;

DELETE FROM certification_public_media;
DELETE FROM certification_materials;
DELETE FROM certifications;

DELETE FROM customer_service_messages;
DELETE FROM feedback_records;
DELETE FROM business_cooperations;

DELETE FROM reconciliation_differences;
DELETE FROM reconciliation_tasks;
DELETE FROM channel_bill_records;
DELETE FROM fund_entries;
DELETE FROM fund_vouchers;
DELETE FROM permanent_ban_payouts;
DELETE FROM wallet_transactions;
DELETE FROM withdrawals;
DELETE FROM recharges;
DELETE FROM alipay_accounts;
DELETE FROM wallet_accounts;

DELETE FROM user_risk_watchlist;
DELETE FROM user_violation_records;
DELETE FROM user_violation_counters;
DELETE FROM security_events;
DELETE FROM analytics_events;
DELETE FROM analytics_ingestion_daily;
DELETE FROM user_login_records;
DELETE FROM login_attempts;
DELETE FROM auth_sessions;
DELETE FROM verification_codes;
DELETE FROM uid_reservations;
DELETE FROM users;

DELETE FROM admin_audit_logs;
DELETE FROM admin_sessions;

ALTER TABLE system_announcement_reads AUTO_INCREMENT = 1;
ALTER TABLE notifications AUTO_INCREMENT = 1;
ALTER TABLE curated_voice_signals AUTO_INCREMENT = 1;
ALTER TABLE curated_voice_calls AUTO_INCREMENT = 1;
ALTER TABLE curated_messages AUTO_INCREMENT = 1;
ALTER TABLE curated_chat_blocks AUTO_INCREMENT = 1;
ALTER TABLE curated_conversations AUTO_INCREMENT = 1;
ALTER TABLE curated_membership_orders AUTO_INCREMENT = 1;
ALTER TABLE curated_memberships AUTO_INCREMENT = 1;
ALTER TABLE curated_membership_materials AUTO_INCREMENT = 1;
ALTER TABLE curated_membership_applications AUTO_INCREMENT = 1;
ALTER TABLE user_communication_blocks AUTO_INCREMENT = 1;
ALTER TABLE inquiry_dispute_events AUTO_INCREMENT = 1;
ALTER TABLE inquiry_end_disputes AUTO_INCREMENT = 1;
ALTER TABLE inquiry_evaluations AUTO_INCREMENT = 1;
ALTER TABLE inquiry_quality_reviews AUTO_INCREMENT = 1;
ALTER TABLE inquiry_timeout_refunds AUTO_INCREMENT = 1;
ALTER TABLE inquiry_audio_appointments AUTO_INCREMENT = 1;
ALTER TABLE inquiry_audio_extensions AUTO_INCREMENT = 1;
ALTER TABLE inquiry_voice_signals AUTO_INCREMENT = 1;
ALTER TABLE inquiry_messages AUTO_INCREMENT = 1;
ALTER TABLE platform_fee_records AUTO_INCREMENT = 1;
ALTER TABLE wallet_income_holds AUTO_INCREMENT = 1;
ALTER TABLE inquiries AUTO_INCREMENT = 1;
ALTER TABLE experience_tips AUTO_INCREMENT = 1;
ALTER TABLE first_experience_rewards AUTO_INCREMENT = 1;
ALTER TABLE certification_public_media AUTO_INCREMENT = 1;
ALTER TABLE certification_materials AUTO_INCREMENT = 1;
ALTER TABLE certifications AUTO_INCREMENT = 1;
ALTER TABLE customer_service_messages AUTO_INCREMENT = 1;
ALTER TABLE feedback_records AUTO_INCREMENT = 1;
ALTER TABLE business_cooperations AUTO_INCREMENT = 1;
ALTER TABLE reconciliation_differences AUTO_INCREMENT = 1;
ALTER TABLE reconciliation_tasks AUTO_INCREMENT = 1;
ALTER TABLE channel_bill_records AUTO_INCREMENT = 1;
ALTER TABLE fund_entries AUTO_INCREMENT = 1;
ALTER TABLE fund_vouchers AUTO_INCREMENT = 1;
ALTER TABLE permanent_ban_payouts AUTO_INCREMENT = 1;
ALTER TABLE wallet_transactions AUTO_INCREMENT = 1;
ALTER TABLE withdrawals AUTO_INCREMENT = 1;
ALTER TABLE recharges AUTO_INCREMENT = 1;
ALTER TABLE alipay_accounts AUTO_INCREMENT = 1;
ALTER TABLE wallet_accounts AUTO_INCREMENT = 1;
ALTER TABLE user_violation_records AUTO_INCREMENT = 1;
ALTER TABLE security_events AUTO_INCREMENT = 1;
ALTER TABLE analytics_events AUTO_INCREMENT = 1;
ALTER TABLE user_login_records AUTO_INCREMENT = 1;
ALTER TABLE login_attempts AUTO_INCREMENT = 1;
ALTER TABLE auth_sessions AUTO_INCREMENT = 1;
ALTER TABLE verification_codes AUTO_INCREMENT = 1;
ALTER TABLE uid_reservations AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE admin_audit_logs AUTO_INCREMENT = 1;
ALTER TABLE admin_sessions AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

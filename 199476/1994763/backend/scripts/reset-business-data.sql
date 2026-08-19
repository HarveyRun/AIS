-- Reset dynamic business data while preserving the schema and platform catalog.
-- Preserved: admin_users, admin_system_state, app_test_login_accounts,
-- discovery_categories, discovery_matters, discovery_matter_roles
-- and flyway_schema_history.

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM system_announcement_reads;
DELETE FROM system_announcements;
DELETE FROM notifications;

DELETE FROM inquiry_messages;
DELETE FROM inquiries;

DELETE FROM certification_materials;
DELETE FROM certifications;
DELETE FROM user_jobs;
DELETE FROM discovery_matter_participants;
DELETE FROM discovery_matter_role_people;
DELETE FROM discovery_matter_jobs;
DELETE FROM discovery_experiences;
DELETE FROM jobs;

DELETE FROM customer_service_messages;
DELETE FROM feedback_records;
DELETE FROM business_cooperations;

DELETE FROM wallet_transactions;
DELETE FROM withdrawals;
DELETE FROM recharges;
DELETE FROM bank_cards;
DELETE FROM wallet_accounts;

DELETE FROM user_login_records;
DELETE FROM auth_sessions;
DELETE FROM verification_codes;
DELETE FROM users;

DELETE FROM app_versions;
DELETE FROM admin_audit_logs;
DELETE FROM admin_sessions;

ALTER TABLE system_announcements AUTO_INCREMENT = 1;
ALTER TABLE notifications AUTO_INCREMENT = 1;
ALTER TABLE inquiry_messages AUTO_INCREMENT = 1;
ALTER TABLE inquiries AUTO_INCREMENT = 1;
ALTER TABLE certification_materials AUTO_INCREMENT = 1;
ALTER TABLE certifications AUTO_INCREMENT = 1;
ALTER TABLE user_jobs AUTO_INCREMENT = 1;
ALTER TABLE discovery_matter_participants AUTO_INCREMENT = 1;
ALTER TABLE discovery_matter_role_people AUTO_INCREMENT = 1;
ALTER TABLE discovery_matter_jobs AUTO_INCREMENT = 1;
ALTER TABLE discovery_experiences AUTO_INCREMENT = 1;
ALTER TABLE jobs AUTO_INCREMENT = 1;
ALTER TABLE customer_service_messages AUTO_INCREMENT = 1;
ALTER TABLE feedback_records AUTO_INCREMENT = 1;
ALTER TABLE business_cooperations AUTO_INCREMENT = 1;
ALTER TABLE wallet_transactions AUTO_INCREMENT = 1;
ALTER TABLE withdrawals AUTO_INCREMENT = 1;
ALTER TABLE recharges AUTO_INCREMENT = 1;
ALTER TABLE bank_cards AUTO_INCREMENT = 1;
ALTER TABLE wallet_accounts AUTO_INCREMENT = 1;
ALTER TABLE user_login_records AUTO_INCREMENT = 1;
ALTER TABLE auth_sessions AUTO_INCREMENT = 1;
ALTER TABLE verification_codes AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE app_versions AUTO_INCREMENT = 1;
ALTER TABLE admin_audit_logs AUTO_INCREMENT = 1;
ALTER TABLE admin_sessions AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

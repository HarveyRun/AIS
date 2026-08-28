ALTER TABLE users
    ADD COLUMN deleted_phone_hash CHAR(64) NULL AFTER phone,
    ADD UNIQUE KEY uk_users_deleted_phone_hash (deleted_phone_hash);

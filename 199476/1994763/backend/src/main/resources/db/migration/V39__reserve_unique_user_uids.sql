CREATE TABLE uid_reservations (
    uid VARCHAR(20) PRIMARY KEY,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

INSERT IGNORE INTO uid_reservations(uid)
SELECT uid FROM users;

CREATE TABLE admin_system_state (
    id TINYINT PRIMARY KEY,
    initialized BOOLEAN NOT NULL DEFAULT FALSE,
    initialized_at DATETIME(6) NULL
);

INSERT INTO admin_system_state (id, initialized, initialized_at)
SELECT 1, EXISTS(SELECT 1 FROM admin_users),
       CASE WHEN EXISTS(SELECT 1 FROM admin_users) THEN NOW(6) ELSE NULL END;

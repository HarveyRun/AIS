ALTER TABLE users
    ADD COLUMN platform_intro_required BOOLEAN NOT NULL DEFAULT FALSE
    AFTER last_login_at;

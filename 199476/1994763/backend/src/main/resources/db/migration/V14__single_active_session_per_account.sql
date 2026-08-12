DELETE older_session
FROM auth_sessions older_session
JOIN auth_sessions newer_session
    ON newer_session.user_id = older_session.user_id
   AND newer_session.id > older_session.id;

ALTER TABLE auth_sessions
    ADD UNIQUE KEY uk_auth_sessions_user (user_id);

DELETE older_session
FROM admin_sessions older_session
JOIN admin_sessions newer_session
    ON newer_session.admin_user_id = older_session.admin_user_id
   AND newer_session.id > older_session.id;

ALTER TABLE admin_sessions
    ADD UNIQUE KEY uk_admin_sessions_user (admin_user_id);

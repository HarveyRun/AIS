ALTER TABLE security_events
    DROP FOREIGN KEY fk_security_event_user,
    DROP FOREIGN KEY fk_security_event_admin,
    DROP FOREIGN KEY fk_security_event_reviewer;

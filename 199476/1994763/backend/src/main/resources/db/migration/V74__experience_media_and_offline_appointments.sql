ALTER TABLE job_certification_appointments
    ADD COLUMN appointment_type VARCHAR(20) NOT NULL DEFAULT 'JOB' AFTER user_id,
    ADD COLUMN experience_title VARCHAR(100) NULL AFTER appointment_type,
    ADD COLUMN experience_description VARCHAR(500) NULL AFTER experience_title,
    ADD INDEX idx_certification_appointment_user_type_status
        (user_id, appointment_type, status, appointment_at);


ALTER TABLE job_certification_appointments
    ADD CONSTRAINT uk_job_certification_appointment_time UNIQUE (appointment_at);

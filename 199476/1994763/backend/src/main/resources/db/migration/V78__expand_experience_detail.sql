ALTER TABLE certifications
    MODIFY COLUMN description TEXT NULL;

ALTER TABLE job_certification_appointments
    MODIFY COLUMN experience_description TEXT NULL;

ALTER TABLE certifications
    ADD COLUMN experience_location VARCHAR(100) NULL AFTER description,
    ADD COLUMN experience_start_date DATE NULL AFTER experience_location,
    ADD COLUMN experience_end_date DATE NULL AFTER experience_start_date,
    ADD COLUMN experience_count SMALLINT UNSIGNED NULL AFTER experience_end_date,
    ADD COLUMN experience_role VARCHAR(30) NULL AFTER experience_count,
    ADD COLUMN experience_age_range VARCHAR(20) NULL AFTER experience_role,
    ADD COLUMN experience_education VARCHAR(30) NULL AFTER experience_age_range,
    ADD COLUMN experience_job VARCHAR(50) NULL AFTER experience_education;

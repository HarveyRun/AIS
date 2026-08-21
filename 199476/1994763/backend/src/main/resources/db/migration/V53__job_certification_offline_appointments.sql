CREATE TABLE job_certification_appointments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    appointment_at DATETIME(6) NOT NULL,
    city VARCHAR(30) NOT NULL DEFAULT '北京',
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_job_certification_appointment_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_job_certification_appointment_user_status
        (user_id, status, appointment_at),
    INDEX idx_job_certification_appointment_time
        (appointment_at, status)
);

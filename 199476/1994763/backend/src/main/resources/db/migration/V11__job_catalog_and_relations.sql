CREATE TABLE jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(240) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_job_name (name),
    INDEX idx_job_active_name (active, name)
);

CREATE TABLE user_jobs (
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    certification_id BIGINT NULL,
    capability_description VARCHAR(240) NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, job_id),
    CONSTRAINT fk_user_job_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_job_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT fk_user_job_certification FOREIGN KEY (certification_id) REFERENCES certifications(id) ON DELETE SET NULL,
    INDEX idx_user_job_job (job_id, verified, user_id)
);

CREATE TABLE discovery_matter_jobs (
    matter_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    participation_type VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (matter_id, job_id),
    CONSTRAINT fk_matter_job_matter FOREIGN KEY (matter_id) REFERENCES discovery_matters(id) ON DELETE CASCADE,
    CONSTRAINT fk_matter_job_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT chk_matter_job_type CHECK (participation_type IN ('PRIMARY', 'SUPPORTING')),
    INDEX idx_matter_job_job (job_id, matter_id),
    INDEX idx_matter_job_order (matter_id, participation_type, sort_order, job_id)
);

INSERT IGNORE INTO jobs(name)
SELECT DISTINCT TRIM(title)
FROM certifications
WHERE certification_type='MAIN_JOB' AND title IS NOT NULL AND TRIM(title)<>'';

INSERT IGNORE INTO user_jobs(user_id,job_id,certification_id,capability_description,verified)
SELECT c.user_id,j.id,c.id,u.capability_description,c.status='APPROVED'
FROM certifications c
JOIN jobs j ON j.name=TRIM(c.title)
JOIN users u ON u.id=c.user_id
WHERE c.certification_type='MAIN_JOB';

INSERT IGNORE INTO discovery_matter_jobs(matter_id,job_id,participation_type,sort_order)
SELECT p.matter_id,uj.job_id,
       CASE WHEN SUM(p.participation_type='PRIMARY')>0 THEN 'PRIMARY' ELSE 'SUPPORTING' END,
       MIN(p.sort_order)
FROM discovery_matter_participants p
JOIN user_jobs uj ON uj.user_id=p.user_id
GROUP BY p.matter_id,uj.job_id;

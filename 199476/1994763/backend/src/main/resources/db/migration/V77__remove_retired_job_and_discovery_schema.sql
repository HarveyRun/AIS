-- 岗位现在只是 users.job_title 普通资料字段；发现分类与标准经历库已经退出业务。
-- 先解除认证表的目录外键，再删除整套旧目录结构。
ALTER TABLE certifications
    DROP FOREIGN KEY fk_certification_discovery_experience,
    DROP FOREIGN KEY fk_certification_discovery_category,
    DROP INDEX idx_certification_discovery_experience,
    DROP INDEX idx_certification_discovery,
    DROP CHECK chk_certification_authenticity_percent,
    DROP COLUMN discovery_experience_id,
    DROP COLUMN discovery_category_id,
    DROP COLUMN authenticity_percent,
    DROP COLUMN job_reapply_available_at,
    DROP COLUMN years;

DROP TABLE IF EXISTS discovery_matter_role_people;
DROP TABLE IF EXISTS discovery_matter_roles;
DROP TABLE IF EXISTS discovery_matter_participants;
DROP TABLE IF EXISTS discovery_matter_jobs;
DROP TABLE IF EXISTS discovery_matters;
DROP TABLE IF EXISTS discovery_experiences;
DROP TABLE IF EXISTS discovery_categories;
DROP TABLE IF EXISTS user_jobs;
DROP TABLE IF EXISTS jobs;

ALTER TABLE users
    DROP INDEX idx_user_job_certification_blocked_until,
    DROP COLUMN capability_description,
    DROP COLUMN job_certification_blocked_until;

-- 线下预约表只承载经历认证，清除历史岗位预约并固定新记录的业务类型。
DELETE FROM job_certification_appointments
WHERE appointment_type <> 'EXPERIENCE';

ALTER TABLE job_certification_appointments
    MODIFY COLUMN appointment_type VARCHAR(20) NOT NULL DEFAULT 'EXPERIENCE';

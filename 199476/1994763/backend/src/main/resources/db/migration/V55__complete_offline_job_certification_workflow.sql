ALTER TABLE job_certification_appointments
    DROP INDEX uk_job_certification_appointment_time,
    ADD COLUMN certification_id BIGINT NULL AFTER status,
    ADD COLUMN result_reason VARCHAR(500) NULL AFTER certification_id,
    ADD COLUMN processed_by_admin_id BIGINT NULL AFTER result_reason,
    ADD COLUMN processed_at DATETIME(6) NULL AFTER processed_by_admin_id,
    ADD COLUMN active_slot DATETIME(6)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'BOOKED' THEN appointment_at ELSE NULL END
        ) STORED,
    ADD CONSTRAINT fk_job_certification_appointment_certification
        FOREIGN KEY (certification_id) REFERENCES certifications(id),
    ADD CONSTRAINT fk_job_certification_appointment_admin
        FOREIGN KEY (processed_by_admin_id) REFERENCES admin_users(id),
    ADD UNIQUE INDEX uk_job_certification_active_slot (active_slot);

INSERT INTO admin_permissions(
    code,
    name,
    module_name,
    action_name,
    sort_order,
    system_permission
) VALUES
    ('OFFLINE_APPOINTMENT_VIEW','查看线下认证预约','线下认证','查看',250,TRUE),
    ('OFFLINE_APPOINTMENT_PROCESS','处理线下认证结果','线下认证','处理',260,TRUE);

INSERT IGNORE INTO admin_role_permissions(role_id, permission_id)
SELECT role.id, permission.id
FROM admin_roles role
JOIN admin_permissions permission
WHERE role.code IN (
    'SUPER_ADMIN',
    'GENERAL_ADMIN_L1',
    'GENERAL_ADMIN_L2',
    'CERTIFICATION_ADMIN'
)
AND permission.code IN (
    'OFFLINE_APPOINTMENT_VIEW',
    'OFFLINE_APPOINTMENT_PROCESS'
);

INSERT IGNORE INTO admin_role_permissions(role_id, permission_id)
SELECT role.id, permission.id
FROM admin_roles role
JOIN admin_permissions permission
WHERE role.code = 'GENERAL_ADMIN_L3'
AND permission.code = 'OFFLINE_APPOINTMENT_VIEW';

ALTER TABLE inquiry_audio_appointments
    ADD COLUMN appointment_type VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' AFTER answerer_id,
    ADD INDEX idx_audio_appointment_type_attempts (inquiry_id, appointment_type, id);

CREATE TABLE inquiry_capacity_settings (
    id TINYINT NOT NULL,
    questioner_active_limit INT NOT NULL DEFAULT 1,
    answerer_active_limit INT NOT NULL DEFAULT 3,
    updated_by_admin_id BIGINT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_inquiry_capacity_singleton CHECK (id = 1),
    CONSTRAINT chk_questioner_active_limit CHECK (questioner_active_limit BETWEEN 1 AND 20),
    CONSTRAINT chk_answerer_active_limit CHECK (answerer_active_limit BETWEEN 1 AND 20),
    CONSTRAINT fk_inquiry_capacity_admin
        FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(id)
);

INSERT INTO inquiry_capacity_settings(id, questioner_active_limit, answerer_active_limit)
VALUES (1, 1, 3);

INSERT INTO admin_permissions(code,name,module_name,action_name,sort_order,system_permission) VALUES
('INQUIRY_CAPACITY_VIEW','查看询问容量','询问规则','查看',1120,TRUE),
('INQUIRY_CAPACITY_EDIT','修改询问容量','询问规则','修改',1130,TRUE);

INSERT IGNORE INTO admin_role_permissions(role_id,permission_id)
SELECT role.id,permission.id
FROM admin_roles role
JOIN admin_permissions permission
  ON permission.code IN ('INQUIRY_CAPACITY_VIEW','INQUIRY_CAPACITY_EDIT')
WHERE role.code IN ('SUPER_ADMIN','GENERAL_ADMIN_L1');

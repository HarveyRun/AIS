-- 经历认证不再提供线下预约；保留历史预约表中的既有记录，仅停用对应后台权限。
UPDATE admin_permissions
SET active = FALSE
WHERE code IN ('OFFLINE_APPOINTMENT_VIEW', 'OFFLINE_APPOINTMENT_PROCESS');

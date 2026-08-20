ALTER TABLE admin_users
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;

CREATE TABLE admin_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    module_name VARCHAR(60) NOT NULL,
    action_name VARCHAR(60) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    system_permission BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_admin_permission_module (module_name, sort_order, id)
);

CREATE TABLE admin_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    level_no INT NOT NULL DEFAULT 100,
    description VARCHAR(300) NULL,
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_admin_role_level (level_no, id)
);

CREATE TABLE admin_role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_admin_role_permission_role FOREIGN KEY (role_id) REFERENCES admin_roles(id),
    CONSTRAINT fk_admin_role_permission_permission FOREIGN KEY (permission_id) REFERENCES admin_permissions(id)
);

CREATE TABLE admin_user_roles (
    admin_user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (admin_user_id, role_id),
    CONSTRAINT fk_admin_user_role_user FOREIGN KEY (admin_user_id) REFERENCES admin_users(id),
    CONSTRAINT fk_admin_user_role_role FOREIGN KEY (role_id) REFERENCES admin_roles(id)
);

INSERT INTO admin_permissions(code,name,module_name,action_name,sort_order,system_permission) VALUES
('DASHBOARD_VIEW','查看概览','概览','查看',10,TRUE),
('USER_VIEW','查看用户','用户管理','查看',100,TRUE),
('USER_STATUS','处理用户状态','用户管理','封禁/解禁',110,TRUE),
('CERTIFICATION_VIEW','查看认证','认证审核','查看',200,TRUE),
('CERTIFICATION_REVIEW','审核认证','认证审核','审核',210,TRUE),
('CERTIFICATION_EDIT','编辑认证','认证审核','编辑',220,TRUE),
('CERTIFICATION_TOGGLE','启停认证','认证审核','启用/停用',230,TRUE),
('CERTIFICATION_DELETE','删除认证','认证审核','删除',240,TRUE),
('INQUIRY_VIEW','查看询问','询问管理','查看',300,TRUE),
('WITHDRAWAL_VIEW','查看提现','提现处理','查看',400,TRUE),
('WITHDRAWAL_PROCESS','处理提现','提现处理','处理',410,TRUE),
('WITHDRAWAL_EXPORT','导出提现','提现处理','导出',420,TRUE),
('CUSTOMER_SERVICE_VIEW','查看客服会话','在线客服','查看',500,TRUE),
('CUSTOMER_SERVICE_READ','标记客服消息已读','在线客服','已读',510,TRUE),
('CUSTOMER_SERVICE_REPLY','回复客服消息','在线客服','回复',520,TRUE),
('JOB_VIEW','查看岗位','岗位管理','查看',600,TRUE),
('JOB_CREATE','新增岗位','岗位管理','新增',610,TRUE),
('JOB_EDIT','编辑岗位','岗位管理','编辑',620,TRUE),
('JOB_DELETE','删除岗位','岗位管理','删除',630,TRUE),
('EXPERIENCE_VIEW','查看经历','经历管理','查看',700,TRUE),
('EXPERIENCE_CREATE','新增经历','经历管理','新增',710,TRUE),
('EXPERIENCE_EDIT','编辑经历','经历管理','编辑',720,TRUE),
('EXPERIENCE_DELETE','删除经历','经历管理','删除',730,TRUE),
('EXPERIENCE_RELATE_USER','关联经历用户','经历管理','关联用户',740,TRUE),
('DISCOVERY_VIEW','查看分类','分类管理','查看',800,TRUE),
('DISCOVERY_CREATE','新增分类内容','分类管理','新增',810,TRUE),
('DISCOVERY_EDIT','编辑分类内容','分类管理','编辑',820,TRUE),
('DISCOVERY_DELETE','删除分类内容','分类管理','删除',830,TRUE),
('ANNOUNCEMENT_VIEW','查看通知','通知管理','查看',900,TRUE),
('ANNOUNCEMENT_CREATE','新增通知','通知管理','新增',910,TRUE),
('ANNOUNCEMENT_EDIT','编辑通知','通知管理','编辑',920,TRUE),
('ANNOUNCEMENT_PUBLISH','发布通知','通知管理','发布',930,TRUE),
('ANNOUNCEMENT_WITHDRAW','撤回通知','通知管理','撤回',940,TRUE),
('ANNOUNCEMENT_DELETE','删除通知','通知管理','删除',950,TRUE),
('BANNER_VIEW','查看轮播','首页轮播','查看',1000,TRUE),
('BANNER_UPLOAD','上传轮播图片','首页轮播','上传图片',1010,TRUE),
('BANNER_CREATE','新增轮播','首页轮播','新增',1020,TRUE),
('BANNER_EDIT','编辑轮播','首页轮播','编辑',1030,TRUE),
('BANNER_TOGGLE','启停轮播','首页轮播','启用/停用',1040,TRUE),
('BANNER_DELETE','删除轮播','首页轮播','删除',1050,TRUE),
('PLATFORM_FEE_VIEW','查看服务费率','平台服务费','查看',1100,TRUE),
('PLATFORM_FEE_EDIT','修改服务费率','平台服务费','修改',1110,TRUE),
('FEEDBACK_VIEW','查看投诉反馈','投诉反馈','查看',1200,TRUE),
('FEEDBACK_PROCESS','处理投诉反馈','投诉反馈','处理',1210,TRUE),
('COOPERATION_VIEW','查看商务合作','商务合作','查看',1300,TRUE),
('COOPERATION_PROCESS','处理商务合作','商务合作','处理',1310,TRUE),
('APP_VERSION_VIEW','查看App版本','App版本管理','查看',1400,TRUE),
('APP_VERSION_CREATE','新增App版本','App版本管理','新增',1410,TRUE),
('APP_VERSION_EDIT','编辑App版本','App版本管理','编辑',1420,TRUE),
('APP_VERSION_PUBLISH','发布/撤回App版本','App版本管理','发布/撤回',1430,TRUE),
('APP_VERSION_DELETE','删除App版本','App版本管理','删除',1440,TRUE),
('APP_TEST_ACCOUNT_VIEW','查看App超级账号','App超级账号','查看',1500,TRUE),
('APP_TEST_ACCOUNT_CREATE','新增App超级账号','App超级账号','新增',1510,TRUE),
('APP_TEST_ACCOUNT_EDIT','编辑App超级账号','App超级账号','编辑',1520,TRUE),
('APP_TEST_ACCOUNT_DELETE','删除App超级账号','App超级账号','删除',1530,TRUE),
('AUDIT_LOG_VIEW','查看操作记录','操作记录','查看',1600,TRUE),
('SECURITY_EVENT_VIEW','查看安全事件','安全事件','查看',1700,TRUE),
('SECURITY_EVENT_REVIEW','处理安全事件','安全事件','处理',1710,TRUE),
('ADMIN_USER_VIEW','查看后台账号','后台账号','查看',1800,TRUE),
('ADMIN_USER_CREATE','新增后台账号','后台账号','新增',1810,TRUE),
('ADMIN_USER_EDIT','编辑后台账号','后台账号','编辑',1820,TRUE),
('ADMIN_USER_DELETE','删除后台账号','后台账号','删除',1830,TRUE),
('ADMIN_USER_RESET_PASSWORD','重置后台密码','后台账号','重置密码',1840,TRUE),
('ADMIN_USER_ASSIGN_ROLE','配置后台账号角色','后台账号','配置角色',1850,TRUE),
('ROLE_VIEW','查看角色','角色管理','查看',1900,TRUE),
('ROLE_CREATE','新增角色','角色管理','新增',1910,TRUE),
('ROLE_EDIT','编辑角色','角色管理','编辑',1920,TRUE),
('ROLE_DELETE','删除角色','角色管理','删除',1930,TRUE),
('ROLE_ASSIGN_PERMISSION','配置角色权限','角色管理','配置权限',1940,TRUE),
('PERMISSION_VIEW','查看权限','权限管理','查看',2000,TRUE),
('PERMISSION_CREATE','新增权限','权限管理','新增',2010,TRUE),
('PERMISSION_EDIT','编辑权限','权限管理','编辑',2020,TRUE),
('PERMISSION_DELETE','删除权限','权限管理','删除',2030,TRUE);

INSERT INTO admin_roles(code,name,level_no,description,system_role) VALUES
('SUPER_ADMIN','超级管理员',0,'拥有平台全部权限，负责后台账号与权限体系',TRUE),
('GENERAL_ADMIN_L1','一级普通管理员',10,'负责大部分日常业务，可处理资金业务但不能管理后台权限体系',TRUE),
('GENERAL_ADMIN_L2','二级普通管理员',20,'负责用户、审核、内容和客服业务，不可处理资金及系统安全配置',TRUE),
('GENERAL_ADMIN_L3','三级普通管理员',30,'以业务查看为主，可承担基础客服工作',TRUE),
('FINANCE_ADMIN','财务管理员',40,'负责提现、服务费率和资金记录',TRUE),
('CERTIFICATION_ADMIN','认证审核员',50,'负责身份、岗位和经历认证',TRUE),
('CUSTOMER_SERVICE_ADMIN','客服管理员',60,'负责客服、反馈和商务合作',TRUE),
('CONTENT_ADMIN','内容运营',70,'负责岗位、经历、分类、通知和轮播内容',TRUE);

INSERT INTO admin_role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM admin_roles r JOIN admin_permissions p
WHERE r.code='SUPER_ADMIN';

INSERT INTO admin_role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM admin_roles r JOIN admin_permissions p
WHERE r.code='GENERAL_ADMIN_L1'
  AND p.code NOT LIKE 'ADMIN_USER_%'
  AND p.code NOT LIKE 'ROLE_%'
  AND p.code NOT LIKE 'PERMISSION_%'
  AND p.code NOT IN ('SECURITY_EVENT_REVIEW');

INSERT INTO admin_role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM admin_roles r JOIN admin_permissions p
WHERE r.code='GENERAL_ADMIN_L2'
  AND p.code IN (
    'DASHBOARD_VIEW','USER_VIEW','USER_STATUS','CERTIFICATION_VIEW','CERTIFICATION_REVIEW',
    'CERTIFICATION_EDIT','CERTIFICATION_TOGGLE','INQUIRY_VIEW','CUSTOMER_SERVICE_VIEW',
    'CUSTOMER_SERVICE_READ','CUSTOMER_SERVICE_REPLY','JOB_VIEW','JOB_CREATE','JOB_EDIT',
    'EXPERIENCE_VIEW','EXPERIENCE_CREATE','EXPERIENCE_EDIT','EXPERIENCE_RELATE_USER',
    'DISCOVERY_VIEW','DISCOVERY_CREATE','DISCOVERY_EDIT','ANNOUNCEMENT_VIEW','ANNOUNCEMENT_CREATE',
    'ANNOUNCEMENT_EDIT','ANNOUNCEMENT_PUBLISH','BANNER_VIEW','BANNER_UPLOAD','BANNER_CREATE',
    'BANNER_EDIT','BANNER_TOGGLE','FEEDBACK_VIEW','FEEDBACK_PROCESS','COOPERATION_VIEW',
    'COOPERATION_PROCESS','AUDIT_LOG_VIEW'
  );

INSERT INTO admin_role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM admin_roles r JOIN admin_permissions p
WHERE r.code='GENERAL_ADMIN_L3'
  AND p.code IN (
    'DASHBOARD_VIEW','USER_VIEW','CERTIFICATION_VIEW','INQUIRY_VIEW','WITHDRAWAL_VIEW',
    'CUSTOMER_SERVICE_VIEW','CUSTOMER_SERVICE_READ','CUSTOMER_SERVICE_REPLY','JOB_VIEW',
    'EXPERIENCE_VIEW','DISCOVERY_VIEW','ANNOUNCEMENT_VIEW','BANNER_VIEW','FEEDBACK_VIEW',
    'COOPERATION_VIEW','APP_VERSION_VIEW'
  );

INSERT INTO admin_role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM admin_roles r JOIN admin_permissions p
WHERE (r.code='FINANCE_ADMIN' AND p.code IN ('DASHBOARD_VIEW','USER_VIEW','INQUIRY_VIEW','WITHDRAWAL_VIEW','WITHDRAWAL_PROCESS','WITHDRAWAL_EXPORT','PLATFORM_FEE_VIEW','PLATFORM_FEE_EDIT','AUDIT_LOG_VIEW'))
   OR (r.code='CERTIFICATION_ADMIN' AND p.code IN ('DASHBOARD_VIEW','USER_VIEW','CERTIFICATION_VIEW','CERTIFICATION_REVIEW','CERTIFICATION_EDIT','CERTIFICATION_TOGGLE','CERTIFICATION_DELETE','JOB_VIEW','EXPERIENCE_VIEW','AUDIT_LOG_VIEW'))
   OR (r.code='CUSTOMER_SERVICE_ADMIN' AND p.code IN ('DASHBOARD_VIEW','USER_VIEW','INQUIRY_VIEW','CUSTOMER_SERVICE_VIEW','CUSTOMER_SERVICE_READ','CUSTOMER_SERVICE_REPLY','FEEDBACK_VIEW','FEEDBACK_PROCESS','COOPERATION_VIEW','COOPERATION_PROCESS'))
   OR (r.code='CONTENT_ADMIN' AND (p.code='DASHBOARD_VIEW' OR p.code='AUDIT_LOG_VIEW' OR p.code LIKE 'JOB_%' OR p.code LIKE 'EXPERIENCE_%' OR p.code LIKE 'DISCOVERY_%' OR p.code LIKE 'ANNOUNCEMENT_%' OR p.code LIKE 'BANNER_%'));

INSERT INTO admin_user_roles(admin_user_id,role_id)
SELECT u.id,r.id FROM admin_users u JOIN admin_roles r ON r.code='SUPER_ADMIN'
WHERE u.deleted_at IS NULL;

CREATE TABLE analytics_event_definitions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_name VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    module_name VARCHAR(50) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_analytics_definition_module (module_name, active, id)
);

CREATE TABLE analytics_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    event_name VARCHAR(80) NOT NULL,
    event_version INT NOT NULL DEFAULT 1,
    source_type VARCHAR(20) NOT NULL,
    user_id BIGINT NULL,
    anonymous_id VARCHAR(80) NULL,
    session_id VARCHAR(80) NULL,
    platform VARCHAR(20) NOT NULL,
    app_version VARCHAR(30) NULL,
    page_name VARCHAR(100) NULL,
    source_page VARCHAR(100) NULL,
    environment VARCHAR(20) NOT NULL DEFAULT 'prod',
    test_account BOOLEAN NOT NULL DEFAULT FALSE,
    properties JSON NULL,
    occurred_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_analytics_event_time (event_name, occurred_at),
    INDEX idx_analytics_user_time (user_id, occurred_at),
    INDEX idx_analytics_filter_time (environment, test_account, platform, occurred_at),
    INDEX idx_analytics_session (session_id, occurred_at)
);

CREATE TABLE analytics_ingestion_daily (
    metric_date DATE NOT NULL,
    environment VARCHAR(20) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    attempted_count BIGINT NOT NULL DEFAULT 0,
    accepted_count BIGINT NOT NULL DEFAULT 0,
    duplicate_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (metric_date, environment, platform)
);

INSERT INTO analytics_event_definitions(event_name, display_name, module_name, source_type) VALUES
('app_open','打开App','登录与增长','CLIENT'),
('page_view','浏览页面','页面访问','CLIENT'),
('login_page_view','进入登录页','登录与增长','CLIENT'),
('sms_code_request','获取验证码','登录与增长','SERVER'),
('login_submit','提交登录','登录与增长','CLIENT'),
('login_success','登录成功','登录与增长','SERVER'),
('account_created','创建账户','登录与增长','SERVER'),
('logout_success','退出登录','登录与增长','CLIENT'),
('account_deleted','注销账户','登录与增长','SERVER'),
('home_view','浏览首页','首页','CLIENT'),
('banner_impression','轮播曝光','首页','CLIENT'),
('banner_click','点击轮播','首页','CLIENT'),
('home_search_submit','搜索主职','发现','CLIENT'),
('answerer_card_impression','人员卡片曝光','发现','CLIENT'),
('answerer_card_click','点击人员卡片','发现','CLIENT'),
('matter_entry_click','进入按事情找人','发现','CLIENT'),
('matter_select','选择事情','发现','CLIENT'),
('experience_entry_click','进入按经历找人','发现','CLIENT'),
('experience_select','选择经历','发现','CLIENT'),
('job_filter_select','筛选岗位','发现','CLIENT'),
('people_result_view','人员结果展示','发现','CLIENT'),
('people_result_empty','人员结果为空','发现','CLIENT'),
('search_submit','提交搜索','发现','CLIENT'),
('profile_view','查看个人信息','询问','CLIENT'),
('inquiry_start','开始发起询问','询问','CLIENT'),
('inquiry_submit_click','提交询问','询问','CLIENT'),
('inquiry_created','询问创建成功','询问','SERVER'),
('inquiry_accepted','询问被接受','询问','SERVER'),
('inquiry_rejected','询问被拒绝','询问','SERVER'),
('inquiry_cancelled','询问被撤销','询问','SERVER'),
('inquiry_expired','询问超时','询问','SERVER'),
('inquiry_settled','询问完成结算','询问','SERVER'),
('inquiry_refunded','询问退款','询问','SERVER'),
('chat_open','打开聊天','聊天','CLIENT'),
('message_sent','消息发送成功','聊天','SERVER'),
('image_preview','预览聊天图片','聊天','CLIENT'),
('certification_home_view','进入基础认证','认证','CLIENT'),
('identity_submitted','提交实名认证','认证','SERVER'),
('job_certification_start','开始岗位认证','认证','CLIENT'),
('job_method_select','选择岗位认证方式','认证','CLIENT'),
('job_online_submitted','提交线上岗位认证','认证','SERVER'),
('job_appointment_submitted','预约线下岗位认证','认证','SERVER'),
('experience_create_start','开始添加经历','认证','CLIENT'),
('experience_submitted','提交经历认证','认证','SERVER'),
('certification_approved','认证通过','认证','SERVER'),
('certification_rejected','认证未通过','认证','SERVER'),
('answerer_setting_saved','保存答主设置','答主','SERVER'),
('recharge_start','发起充值','资金','CLIENT'),
('alipay_payment_launch','调起支付宝','资金','CLIENT'),
('recharge_success','充值成功','资金','SERVER'),
('recharge_failed','充值失败','资金','SERVER'),
('alipay_authorization_start','发起支付宝授权','资金','CLIENT'),
('alipay_authorization_result','支付宝授权结果','资金','SERVER'),
('withdrawal_submit','提交提现','资金','SERVER'),
('withdrawal_success','提现成功','资金','SERVER'),
('withdrawal_failed','提现失败','资金','SERVER'),
('invitation_rules_view','查看邀请规则','邀请','CLIENT'),
('invitation_submit','提交邀请码','邀请','SERVER'),
('invitation_approved','邀请审核通过','邀请','SERVER'),
('invitation_rejected','邀请审核驳回','邀请','SERVER'),
('invitation_rewarded','邀请奖励到账','邀请','SERVER'),
('faq_view','查看常见问题','支持','CLIENT'),
('customer_service_open','打开在线客服','支持','CLIENT'),
('feedback_submitted','提交反馈','支持','SERVER'),
('update_prompt_view','展示版本更新','版本','CLIENT'),
('update_action_click','点击立即更新','版本','CLIENT');

INSERT INTO admin_permissions(code, name, module_name, action_name, sort_order, system_permission) VALUES
('ANALYTICS_VIEW','查看运营分析','运营分析','查看',80,TRUE),
('ANALYTICS_FINANCE_VIEW','查看运营资金数据','运营分析','查看资金',81,TRUE),
('ANALYTICS_RAW_VIEW','查看原始埋点','运营分析','查看原始事件',82,TRUE),
('ANALYTICS_EXPORT','导出运营数据','运营分析','导出',83,TRUE);

INSERT IGNORE INTO admin_role_permissions(role_id, permission_id)
SELECT role.id, permission.id
FROM admin_roles role
JOIN admin_permissions permission ON permission.code IN (
    'ANALYTICS_VIEW','ANALYTICS_FINANCE_VIEW','ANALYTICS_RAW_VIEW','ANALYTICS_EXPORT'
)
WHERE role.code IN ('SUPER_ADMIN','GENERAL_ADMIN_L1');

INSERT IGNORE INTO admin_role_permissions(role_id, permission_id)
SELECT role.id, permission.id
FROM admin_roles role
JOIN admin_permissions permission ON permission.code = 'ANALYTICS_VIEW'
WHERE role.code IN (
    'GENERAL_ADMIN_L2','GENERAL_ADMIN_L3','CERTIFICATION_ADMIN',
    'CUSTOMER_SERVICE_ADMIN','CONTENT_ADMIN'
);

INSERT IGNORE INTO admin_role_permissions(role_id, permission_id)
SELECT role.id, permission.id
FROM admin_roles role
JOIN admin_permissions permission ON permission.code IN ('ANALYTICS_VIEW','ANALYTICS_FINANCE_VIEW','ANALYTICS_EXPORT')
WHERE role.code = 'FINANCE_ADMIN';

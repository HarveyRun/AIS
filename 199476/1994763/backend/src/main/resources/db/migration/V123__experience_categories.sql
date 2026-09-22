CREATE TABLE experience_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NULL,
    name VARCHAR(40) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_experience_category_parent FOREIGN KEY (parent_id) REFERENCES experience_categories(id),
    INDEX idx_experience_category_parent (parent_id, deleted_at, enabled, sort_order)
);

ALTER TABLE certifications
    ADD COLUMN experience_category_id BIGINT NULL AFTER description,
    ADD CONSTRAINT fk_certification_experience_category FOREIGN KEY (experience_category_id) REFERENCES experience_categories(id);

INSERT INTO experience_categories(parent_id,name,sort_order) VALUES
(NULL,'常见推荐',10),
(NULL,'住房家居',20),
(NULL,'法律维权',30),
(NULL,'副业创业',40),
(NULL,'健康医疗',50),
(NULL,'政务办事',60),
(NULL,'出国与移民',70),
(NULL,'人生过坎',80),
(NULL,'金钱债务',90),
(NULL,'数字与科技',100),
(NULL,'职业职场',110),
(NULL,'教育升学',120),
(NULL,'生活方式',130),
(NULL,'兴趣爱好',140),
(NULL,'恋爱婚姻',150),
(NULL,'家庭关系',160),
(NULL,'生育育儿',170),
(NULL,'养老尽孝',180);

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '买房卖房' AS name, 10 AS sort_order UNION ALL
    SELECT '邻里纠纷' AS name, 20 AS sort_order UNION ALL
    SELECT '买车用车' AS name, 30 AS sort_order UNION ALL
    SELECT '看病就医' AS name, 40 AS sort_order UNION ALL
    SELECT '副业' AS name, 50 AS sort_order UNION ALL
    SELECT '劳动仲裁' AS name, 60 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='常见推荐' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '新房装修' AS name, 10 AS sort_order UNION ALL
    SELECT '旧房翻新' AS name, 20 AS sort_order UNION ALL
    SELECT '买房卖房' AS name, 30 AS sort_order UNION ALL
    SELECT '购房维权' AS name, 40 AS sort_order UNION ALL
    SELECT '租房' AS name, 50 AS sort_order UNION ALL
    SELECT '同城搬家' AS name, 60 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='住房家居' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '劳动仲裁' AS name, 10 AS sort_order UNION ALL
    SELECT '邻里纠纷' AS name, 20 AS sort_order UNION ALL
    SELECT '合同纠纷' AS name, 30 AS sort_order UNION ALL
    SELECT '婚家事纠纷' AS name, 40 AS sort_order UNION ALL
    SELECT '自己打官司' AS name, 50 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='法律维权' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '实体开店' AS name, 10 AS sort_order UNION ALL
    SELECT '电商带货' AS name, 20 AS sort_order UNION ALL
    SELECT '副业' AS name, 30 AS sort_order UNION ALL
    SELECT '合伙经营' AS name, 40 AS sort_order UNION ALL
    SELECT '创业失败复盘' AS name, 50 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='副业创业' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '看病就医' AS name, 10 AS sort_order UNION ALL
    SELECT '手术与陪护' AS name, 20 AS sort_order UNION ALL
    SELECT '慢病管理' AS name, 30 AS sort_order UNION ALL
    SELECT '心理健康' AS name, 40 AS sort_order UNION ALL
    SELECT '戒瘾' AS name, 50 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='健康医疗' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '证件办理' AS name, 10 AS sort_order UNION ALL
    SELECT '落户办理' AS name, 20 AS sort_order UNION ALL
    SELECT '社保公积金' AS name, 30 AS sort_order UNION ALL
    SELECT '车驾管' AS name, 40 AS sort_order UNION ALL
    SELECT '不动产登记' AS name, 50 AS sort_order UNION ALL
    SELECT '投诉与信访' AS name, 60 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='政务办事' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '留学申请' AS name, 10 AS sort_order UNION ALL
    SELECT '留学生活' AS name, 20 AS sort_order UNION ALL
    SELECT '移民渠道' AS name, 30 AS sort_order UNION ALL
    SELECT '签证办理' AS name, 40 AS sort_order UNION ALL
    SELECT '海外工作' AS name, 50 AS sort_order UNION ALL
    SELECT '海外生活' AS name, 60 AS sort_order UNION ALL
    SELECT '回国发展' AS name, 70 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='出国与移民' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '重大疾病' AS name, 10 AS sort_order UNION ALL
    SELECT '意外与灾祸' AS name, 20 AS sort_order UNION ALL
    SELECT '破产与征信' AS name, 30 AS sort_order UNION ALL
    SELECT '丧亲失友' AS name, 40 AS sort_order UNION ALL
    SELECT '回归社会' AS name, 50 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='人生过坎' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '投资理财' AS name, 10 AS sort_order UNION ALL
    SELECT '保险' AS name, 20 AS sort_order UNION ALL
    SELECT '房贷车贷' AS name, 30 AS sort_order UNION ALL
    SELECT '被骗维权' AS name, 40 AS sort_order UNION ALL
    SELECT '负债逾期复盘' AS name, 50 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='金钱债务' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '数字消费与避坑' AS name, 10 AS sort_order UNION ALL
    SELECT '智能生活' AS name, 20 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='数字与科技' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '考公考编' AS name, 10 AS sort_order UNION ALL
    SELECT '求职面试' AS name, 20 AS sort_order UNION ALL
    SELECT '跳槽转行' AS name, 30 AS sort_order UNION ALL
    SELECT '职场进阶' AS name, 40 AS sort_order UNION ALL
    SELECT '失业与补偿' AS name, 50 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='职业职场' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '中小学择校' AS name, 10 AS sort_order UNION ALL
    SELECT '中高考' AS name, 20 AS sort_order UNION ALL
    SELECT '志愿与选校' AS name, 30 AS sort_order UNION ALL
    SELECT '考研保研' AS name, 40 AS sort_order UNION ALL
    SELECT '艺考' AS name, 50 AS sort_order UNION ALL
    SELECT '成人学历提升' AS name, 60 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='教育升学' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '养宠' AS name, 10 AS sort_order UNION ALL
    SELECT '买车用车' AS name, 20 AS sort_order UNION ALL
    SELECT '移居旅居' AS name, 30 AS sort_order UNION ALL
    SELECT '独居生活' AS name, 40 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='生活方式' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '兴趣爱好' AS name, 10 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='兴趣爱好' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '恋爱脱单' AS name, 10 AS sort_order UNION ALL
    SELECT '婚前实务' AS name, 20 AS sort_order UNION ALL
    SELECT '婚礼筹备' AS name, 30 AS sort_order UNION ALL
    SELECT '婚姻经营' AS name, 40 AS sort_order UNION ALL
    SELECT '离婚与重启' AS name, 50 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='恋爱婚姻' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '婆媳与亲家' AS name, 10 AS sort_order UNION ALL
    SELECT '原生家庭' AS name, 20 AS sort_order UNION ALL
    SELECT '亲戚往来' AS name, 30 AS sort_order UNION ALL
    SELECT '多子女家庭' AS name, 40 AS sort_order UNION ALL
    SELECT '重组家庭' AS name, 50 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='家庭关系' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '备孕怀孕' AS name, 10 AS sort_order UNION ALL
    SELECT '生产月子' AS name, 20 AS sort_order UNION ALL
    SELECT '婴幼儿养育' AS name, 30 AS sort_order UNION ALL
    SELECT '单亲养育' AS name, 40 AS sort_order UNION ALL
    SELECT '幼教择园' AS name, 50 AS sort_order UNION ALL
    SELECT '青春期教养' AS name, 60 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='生育育儿' AND p.deleted_at IS NULL;

INSERT INTO experience_categories(parent_id,name,sort_order)
SELECT p.id,v.name,v.sort_order FROM experience_categories p JOIN (
    SELECT '父母养老安排' AS name, 10 AS sort_order UNION ALL
    SELECT '养老机构' AS name, 20 AS sort_order UNION ALL
    SELECT '失能照护' AS name, 30 AS sort_order UNION ALL
    SELECT '遗产与身后事' AS name, 40 AS sort_order
) v WHERE p.parent_id IS NULL AND p.name='养老尽孝' AND p.deleted_at IS NULL;

INSERT INTO admin_permissions(code,name,module_name,action_name,sort_order,system_permission) VALUES
('EXPERIENCE_CATEGORY_VIEW','查看经历分类','经历分类','查看',2600,TRUE),
('EXPERIENCE_CATEGORY_CREATE','新增经历分类','经历分类','新增',2610,TRUE),
('EXPERIENCE_CATEGORY_EDIT','编辑/启停经历分类','经历分类','编辑/启停',2620,TRUE),
('EXPERIENCE_CATEGORY_DELETE','删除经历分类','经历分类','删除',2630,TRUE);

INSERT INTO admin_role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM admin_roles r JOIN admin_permissions p ON p.code LIKE 'EXPERIENCE_CATEGORY_%'
WHERE r.code IN ('SUPER_ADMIN','GENERAL_ADMIN_L1','CONTENT_ADMIN')
   OR (r.code='GENERAL_ADMIN_L2' AND p.code <> 'EXPERIENCE_CATEGORY_DELETE')
   OR (r.code IN ('GENERAL_ADMIN_L3','CERTIFICATION_ADMIN') AND p.code='EXPERIENCE_CATEGORY_VIEW');

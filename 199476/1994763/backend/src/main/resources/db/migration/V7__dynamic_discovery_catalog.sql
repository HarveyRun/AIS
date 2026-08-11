CREATE TABLE discovery_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    main_category VARCHAR(30) NOT NULL,
    name VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_discovery_category (main_category, name),
    INDEX idx_discovery_category_order (main_category, active, sort_order, id)
);

CREATE TABLE discovery_matters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_discovery_matter_category FOREIGN KEY (category_id) REFERENCES discovery_categories(id),
    UNIQUE KEY uk_discovery_matter (category_id, title),
    INDEX idx_discovery_matter_order (category_id, active, sort_order, id)
);

ALTER TABLE certifications ADD COLUMN discovery_category_id BIGINT NULL AFTER category;
ALTER TABLE certifications
    ADD CONSTRAINT fk_certification_discovery_category
        FOREIGN KEY (discovery_category_id) REFERENCES discovery_categories(id),
    ADD INDEX idx_certification_discovery (status, category, discovery_category_id);

INSERT INTO discovery_categories(main_category, name, sort_order) VALUES
('LIFE', '房屋与居住', 10), ('LIFE', '家庭与照护', 20), ('LIFE', '买卖与纠纷', 30),
('WORK', '劳动关系', 10), ('WORK', '经营与办事', 20), ('WORK', '制作与维护', 30),
('ENTERTAINMENT', '聚会与活动', 10), ('ENTERTAINMENT', '兴趣与出行', 20);

INSERT INTO discovery_matters(category_id, title, sort_order)
SELECT id, '家里经常跳闸', 10 FROM discovery_categories WHERE main_category='LIFE' AND name='房屋与居住'
UNION ALL SELECT id, '老房卫生间漏水怎么排查', 20 FROM discovery_categories WHERE main_category='LIFE' AND name='房屋与居住'
UNION ALL SELECT id, '旧房装修先拆哪里再做哪里', 30 FROM discovery_categories WHERE main_category='LIFE' AND name='房屋与居住'
UNION ALL SELECT id, '如何找靠谱的日常保洁', 40 FROM discovery_categories WHERE main_category='LIFE' AND name='房屋与居住'
UNION ALL SELECT id, '客厅杂物太多怎么整理收纳', 50 FROM discovery_categories WHERE main_category='LIFE' AND name='房屋与居住'
UNION ALL SELECT id, '老人出院后在家怎么照顾', 10 FROM discovery_categories WHERE main_category='LIFE' AND name='家庭与照护'
UNION ALL SELECT id, '第一次独自照顾新生儿要注意什么', 20 FROM discovery_categories WHERE main_category='LIFE' AND name='家庭与照护'
UNION ALL SELECT id, '邻居夜间噪声影响休息怎么办', 30 FROM discovery_categories WHERE main_category='LIFE' AND name='家庭与照护'
UNION ALL SELECT id, '孩子第一次上幼儿园要准备什么', 40 FROM discovery_categories WHERE main_category='LIFE' AND name='家庭与照护'
UNION ALL SELECT id, '买二手房前应该检查哪些问题', 10 FROM discovery_categories WHERE main_category='LIFE' AND name='买卖与纠纷'
UNION ALL SELECT id, '退租后房东不退押金怎么办', 20 FROM discovery_categories WHERE main_category='LIFE' AND name='买卖与纠纷'
UNION ALL SELECT id, '网购商品与描述不符怎么处理', 30 FROM discovery_categories WHERE main_category='LIFE' AND name='买卖与纠纷'
UNION ALL SELECT id, '车辆发生轻微剐蹭后怎么处理', 40 FROM discovery_categories WHERE main_category='LIFE' AND name='买卖与纠纷'
UNION ALL SELECT id, '公司口头通知辞退应该怎么办', 10 FROM discovery_categories WHERE main_category='WORK' AND name='劳动关系'
UNION ALL SELECT id, '申请劳动仲裁需要准备哪些材料', 20 FROM discovery_categories WHERE main_category='WORK' AND name='劳动关系'
UNION ALL SELECT id, '入职后发现工作内容与约定不符怎么办', 30 FROM discovery_categories WHERE main_category='WORK' AND name='劳动关系'
UNION ALL SELECT id, '离职后公司拖欠工资怎么办', 40 FROM discovery_categories WHERE main_category='WORK' AND name='劳动关系'
UNION ALL SELECT id, '第一次开餐饮店要办理哪些手续', 10 FROM discovery_categories WHERE main_category='WORK' AND name='经营与办事'
UNION ALL SELECT id, '门店客流减少该从哪里排查', 20 FROM discovery_categories WHERE main_category='WORK' AND name='经营与办事'
UNION ALL SELECT id, '签商铺租赁合同要注意哪些条款', 30 FROM discovery_categories WHERE main_category='WORK' AND name='经营与办事'
UNION ALL SELECT id, '小店第一次做线上推广怎么开始', 40 FROM discovery_categories WHERE main_category='WORK' AND name='经营与办事'
UNION ALL SELECT id, '想做一个预约登记小程序该怎么准备', 10 FROM discovery_categories WHERE main_category='WORK' AND name='制作与维护'
UNION ALL SELECT id, '开业宣传海报需要提供哪些内容', 20 FROM discovery_categories WHERE main_category='WORK' AND name='制作与维护'
UNION ALL SELECT id, '手机拍的视频怎么剪成一分钟短片', 30 FROM discovery_categories WHERE main_category='WORK' AND name='制作与维护'
UNION ALL SELECT id, '电脑开机很慢应该先排查什么', 40 FROM discovery_categories WHERE main_category='WORK' AND name='制作与维护'
UNION ALL SELECT id, '如何安排十个人的周末聚会', 10 FROM discovery_categories WHERE main_category='ENTERTAINMENT' AND name='聚会与活动'
UNION ALL SELECT id, '第一次组织户外露营要准备什么', 20 FROM discovery_categories WHERE main_category='ENTERTAINMENT' AND name='聚会与活动'
UNION ALL SELECT id, '如何给孩子办一场生日聚会', 30 FROM discovery_categories WHERE main_category='ENTERTAINMENT' AND name='聚会与活动'
UNION ALL SELECT id, '公司年会节目怎么安排', 40 FROM discovery_categories WHERE main_category='ENTERTAINMENT' AND name='聚会与活动'
UNION ALL SELECT id, '第一次去海边钓鱼要准备什么', 10 FROM discovery_categories WHERE main_category='ENTERTAINMENT' AND name='兴趣与出行'
UNION ALL SELECT id, '如何挑选适合新手的露营装备', 20 FROM discovery_categories WHERE main_category='ENTERTAINMENT' AND name='兴趣与出行'
UNION ALL SELECT id, '带老人和孩子自驾游怎么安排', 30 FROM discovery_categories WHERE main_category='ENTERTAINMENT' AND name='兴趣与出行'
UNION ALL SELECT id, '第一次学摄影该买什么设备', 40 FROM discovery_categories WHERE main_category='ENTERTAINMENT' AND name='兴趣与出行';

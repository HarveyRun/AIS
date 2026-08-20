CREATE TABLE home_banners (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    display_mode VARCHAR(20) NOT NULL,
    label_text VARCHAR(30) NULL,
    title VARCHAR(80) NULL,
    description VARCHAR(200) NULL,
    image_url VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    updated_by_admin_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_home_banner_updated_admin
        FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_home_banner_public (deleted, enabled, sort_order, id),
    INDEX idx_home_banner_admin (deleted, sort_order, id)
);

INSERT INTO home_banners (
    display_mode,
    label_text,
    title,
    description,
    sort_order,
    enabled
) VALUES
    ('TEXT_ONLY', '买房装修', '大多数人都绕不开', '先问过来人，别稀里糊涂花钱', 10, TRUE),
    ('TEXT_ONLY', '职场变动', '谁都可能碰上', '先听听别人怎么走过来的，心里就有底了', 20, TRUE),
    ('TEXT_ONLY', '家庭照顾', '没人天生就会照顾', '听听过来人的经验', 30, TRUE);

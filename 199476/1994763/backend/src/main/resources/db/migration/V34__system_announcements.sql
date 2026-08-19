CREATE TABLE system_announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    scheduled_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    audience_user_id_max BIGINT NULL,
    recipient_count BIGINT NOT NULL DEFAULT 0,
    created_by_admin_id BIGINT NOT NULL,
    updated_by_admin_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    CONSTRAINT fk_announcement_created_admin
        FOREIGN KEY (created_by_admin_id) REFERENCES admin_users(id),
    CONSTRAINT fk_announcement_updated_admin
        FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(id),
    INDEX idx_announcement_admin_list (deleted_at, status, created_at),
    INDEX idx_announcement_user_list (
        status,
        published_at,
        audience_user_id_max,
        deleted_at
    )
);

CREATE TABLE system_announcement_reads (
    announcement_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (announcement_id, user_id),
    CONSTRAINT fk_announcement_read_announcement
        FOREIGN KEY (announcement_id) REFERENCES system_announcements(id),
    CONSTRAINT fk_announcement_read_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_announcement_read_user (user_id, read_at)
);

CREATE TABLE app_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    platform VARCHAR(20) NOT NULL,
    version_name VARCHAR(30) NOT NULL,
    version_code INT NOT NULL,
    minimum_supported_version_code INT NOT NULL,
    title VARCHAR(80) NOT NULL,
    update_content VARCHAR(1000) NOT NULL,
    download_url VARCHAR(500) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at DATETIME(6) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    updated_by_admin_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_app_versions_release (platform, published, deleted, version_code),
    CONSTRAINT fk_app_versions_updated_admin
        FOREIGN KEY (updated_by_admin_id) REFERENCES admin_users(id)
);

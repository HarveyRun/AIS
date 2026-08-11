CREATE TABLE discovery_matter_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    matter_id BIGINT NOT NULL,
    role_name VARCHAR(80) NOT NULL,
    role_type VARCHAR(20) NOT NULL,
    role_description VARCHAR(240) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_discovery_matter_role_matter
        FOREIGN KEY (matter_id) REFERENCES discovery_matters(id) ON DELETE CASCADE,
    CONSTRAINT chk_discovery_matter_role_type
        CHECK (role_type IN ('REQUIRED', 'OPTIONAL')),
    UNIQUE KEY uk_discovery_matter_role (matter_id, role_type, role_name),
    INDEX idx_discovery_matter_role_order (matter_id, role_type, sort_order, id)
);

CREATE TABLE discovery_matter_role_people (
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (role_id, user_id),
    CONSTRAINT fk_discovery_role_person_role
        FOREIGN KEY (role_id) REFERENCES discovery_matter_roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_discovery_role_person_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_discovery_role_person_user (user_id, role_id)
);

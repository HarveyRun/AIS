CREATE TABLE discovery_experiences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_discovery_experience_category
        FOREIGN KEY (category_id) REFERENCES discovery_categories(id),
    CONSTRAINT uk_discovery_experience_category_name UNIQUE (category_id, name),
    INDEX idx_discovery_experience_catalog (active, category_id, name)
);

INSERT INTO discovery_experiences(category_id, name, active)
SELECT DISTINCT discovery_category_id, title, TRUE
FROM certifications
WHERE category = 'EXPERIENCE'
  AND status = 'APPROVED'
  AND discovery_category_id IS NOT NULL;

ALTER TABLE certifications
    ADD COLUMN discovery_experience_id BIGINT NULL AFTER discovery_category_id,
    ADD CONSTRAINT fk_certification_discovery_experience
        FOREIGN KEY (discovery_experience_id) REFERENCES discovery_experiences(id),
    ADD INDEX idx_certification_discovery_experience
        (category, status, discovery_experience_id, user_id);

UPDATE certifications c
JOIN discovery_experiences e
  ON e.category_id = c.discovery_category_id
 AND e.name = c.title
SET c.discovery_experience_id = e.id
WHERE c.category = 'EXPERIENCE'
  AND c.status = 'APPROVED';

ALTER TABLE certifications
    ADD COLUMN material_support_score TINYINT UNSIGNED NULL AFTER media_processed_at,
    ADD COLUMN common_relevance_score TINYINT UNSIGNED NULL AFTER material_support_score,
    ADD COLUMN learnability_score TINYINT UNSIGNED NULL AFTER common_relevance_score,
    ADD COLUMN clarity_score TINYINT UNSIGNED NULL AFTER learnability_score,
    ADD COLUMN logic_consistency_score TINYINT UNSIGNED NULL AFTER clarity_score,
    ADD COLUMN reference_index TINYINT UNSIGNED NULL AFTER logic_consistency_score,
    ADD CONSTRAINT chk_certification_material_support_score CHECK (material_support_score IS NULL OR material_support_score BETWEEN 0 AND 10),
    ADD CONSTRAINT chk_certification_common_relevance_score CHECK (common_relevance_score IS NULL OR common_relevance_score BETWEEN 0 AND 10),
    ADD CONSTRAINT chk_certification_learnability_score CHECK (learnability_score IS NULL OR learnability_score BETWEEN 0 AND 10),
    ADD CONSTRAINT chk_certification_clarity_score CHECK (clarity_score IS NULL OR clarity_score BETWEEN 0 AND 10),
    ADD CONSTRAINT chk_certification_logic_consistency_score CHECK (logic_consistency_score IS NULL OR logic_consistency_score BETWEEN 0 AND 10),
    ADD CONSTRAINT chk_certification_reference_index CHECK (reference_index IS NULL OR reference_index BETWEEN 0 AND 100);

CREATE TABLE experience_review_score_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    certification_id BIGINT NOT NULL,
    reviewer_admin_id BIGINT NOT NULL,
    material_support_score TINYINT UNSIGNED NOT NULL,
    common_relevance_score TINYINT UNSIGNED NOT NULL,
    learnability_score TINYINT UNSIGNED NOT NULL,
    clarity_score TINYINT UNSIGNED NOT NULL,
    logic_consistency_score TINYINT UNSIGNED NOT NULL,
    reference_index TINYINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_experience_score_certification
        FOREIGN KEY (certification_id) REFERENCES certifications(id),
    CONSTRAINT fk_experience_score_reviewer
        FOREIGN KEY (reviewer_admin_id) REFERENCES admin_users(id),
    CONSTRAINT chk_experience_score_material CHECK (material_support_score BETWEEN 0 AND 10),
    CONSTRAINT chk_experience_score_common CHECK (common_relevance_score BETWEEN 0 AND 10),
    CONSTRAINT chk_experience_score_learnability CHECK (learnability_score BETWEEN 0 AND 10),
    CONSTRAINT chk_experience_score_clarity CHECK (clarity_score BETWEEN 0 AND 10),
    CONSTRAINT chk_experience_score_logic CHECK (logic_consistency_score BETWEEN 0 AND 10),
    CONSTRAINT chk_experience_score_index CHECK (reference_index BETWEEN 0 AND 100),
    INDEX idx_experience_score_certification (certification_id, id)
);

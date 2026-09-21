ALTER TABLE certifications
    ADD COLUMN information_specificity_score TINYINT UNSIGNED NULL AFTER logic_consistency_score,
    ADD CONSTRAINT chk_certification_information_specificity_score
        CHECK (information_specificity_score IS NULL OR information_specificity_score BETWEEN 0 AND 10);

ALTER TABLE experience_review_score_history
    ADD COLUMN information_specificity_score TINYINT UNSIGNED NULL AFTER logic_consistency_score,
    ADD CONSTRAINT chk_experience_score_information_specificity
        CHECK (information_specificity_score IS NULL OR information_specificity_score BETWEEN 0 AND 10);

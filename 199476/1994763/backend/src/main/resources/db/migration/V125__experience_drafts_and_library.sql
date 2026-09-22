CREATE TABLE experience_form_drafts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    draft_key VARCHAR(40) NOT NULL,
    content_json TEXT NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_experience_form_draft_user_key UNIQUE (user_id, draft_key),
    CONSTRAINT fk_experience_form_draft_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE experience_user_library (
    user_id BIGINT NOT NULL,
    certification_id BIGINT NOT NULL,
    favorited_at DATETIME(6) NULL,
    viewed_at DATETIME(6) NULL,
    PRIMARY KEY (user_id, certification_id),
    KEY idx_experience_library_favorites (user_id, favorited_at),
    KEY idx_experience_library_recent (user_id, viewed_at),
    CONSTRAINT fk_experience_library_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_experience_library_certification FOREIGN KEY (certification_id) REFERENCES certifications(id)
);

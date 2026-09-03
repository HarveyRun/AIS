ALTER TABLE certifications
    ADD COLUMN media_processing_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED' AFTER privacy_confirmed_at,
    ADD COLUMN media_processing_error VARCHAR(500) NULL AFTER media_processing_status,
    ADD COLUMN media_processed_at DATETIME(6) NULL AFTER media_processing_error;

CREATE TABLE certification_public_media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    certification_id BIGINT NOT NULL,
    source_material_id BIGINT NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    original_name VARCHAR(500) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    public_selected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_certification_public_media_certification
        FOREIGN KEY (certification_id) REFERENCES certifications(id),
    CONSTRAINT fk_certification_public_media_source_material
        FOREIGN KEY (source_material_id) REFERENCES certification_materials(id),
    INDEX idx_certification_public_media_certification (certification_id, deleted_at, sort_order),
    INDEX idx_certification_public_media_public (certification_id, public_selected, deleted_at)
);

UPDATE certifications
SET media_processing_status = 'PENDING'
WHERE category = 'EXPERIENCE'
  AND status = 'APPROVED'
  AND deleted_at IS NULL;

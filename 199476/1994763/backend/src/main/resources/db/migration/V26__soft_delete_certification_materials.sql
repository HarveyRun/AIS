ALTER TABLE certification_materials
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER created_at,
    ADD INDEX idx_certification_material_available (certification_id, deleted_at, id);

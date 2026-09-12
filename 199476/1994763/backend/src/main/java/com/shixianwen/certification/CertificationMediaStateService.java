package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.storage.StoredFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CertificationMediaStateService {
    private final JdbcTemplate jdbc;

    public CertificationMediaStateService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public SourceArchive begin(Long certificationId) {
        Map<String, Object> certification = jdbc.queryForMap(
            "SELECT c.id,c.user_id AS userId,u.uid,u.account_type AS accountType,c.status " +
                "FROM certifications c JOIN users u ON u.id=c.user_id " +
                "WHERE c.id=? AND c.category='EXPERIENCE' AND c.deleted_at IS NULL FOR UPDATE",
            certificationId
        );
        if (!"APPROVED".equals(certification.get("status"))) {
            throw BusinessException.badRequest("仅已通过的经历可以整理公开内容");
        }
        Map<String, Object> material = jdbc.queryForMap(
            "SELECT id,storage_key AS storageKey,original_name AS originalName,content_type AS contentType " +
                "FROM certification_materials WHERE certification_id=? " +
                "AND material_kind IN ('PROOF_ARCHIVE','ARCHIVE','REVIEW_ORIGINAL_ARCHIVE') " +
                "AND deleted_at IS NULL " +
                "ORDER BY CASE material_kind " +
                "WHEN 'PROOF_ARCHIVE' THEN 1 WHEN 'ARCHIVE' THEN 2 ELSE 3 END,id DESC LIMIT 1",
            certificationId
        );
        jdbc.update(
            "UPDATE certification_public_media SET deleted_at=NOW(6),public_selected=FALSE " +
                "WHERE certification_id=? AND deleted_at IS NULL",
            certificationId
        );
        jdbc.update(
            "UPDATE certifications SET media_processing_status='PROCESSING',media_processing_error=NULL,media_processed_at=NULL WHERE id=?",
            certificationId
        );
        return new SourceArchive(
            ((Number) material.get("id")).longValue(),
            String.valueOf(material.get("storageKey")),
            String.valueOf(material.get("originalName")),
            String.valueOf(material.get("contentType")),
            String.valueOf(certification.get("uid")),
            String.valueOf(certification.get("accountType"))
        );
    }

    @Transactional
    public void add(
        Long certificationId,
        Long sourceMaterialId,
        String mediaType,
        String displayName,
        String originalName,
        StoredFile stored,
        int sortOrder
    ) {
        jdbc.update(
            "INSERT INTO certification_public_media " +
                "(certification_id,source_material_id,media_type,display_name,original_name,storage_key,content_type,file_size,sort_order,public_selected) " +
                "VALUES (?,?,?,?,?,?,?,?,?,FALSE)",
            certificationId,
            sourceMaterialId,
            mediaType,
            displayName,
            originalName,
            stored.storageKey(),
            stored.contentType(),
            stored.size(),
            sortOrder
        );
    }

    @Transactional
    public void ready(Long certificationId) {
        jdbc.update(
            "UPDATE certifications SET media_processing_status='READY',media_processing_error=NULL,media_processed_at=NOW(6) WHERE id=?",
            certificationId
        );
    }

    @Transactional
    public void failed(Long certificationId, String message) {
        jdbc.update(
            "UPDATE certification_public_media SET deleted_at=NOW(6),public_selected=FALSE " +
                "WHERE certification_id=? AND deleted_at IS NULL",
            certificationId
        );
        jdbc.update(
            "UPDATE certifications SET media_processing_status='FAILED',media_processing_error=?,media_processed_at=NOW(6) WHERE id=?",
            message == null ? "证明资料整理失败" : message.substring(0, Math.min(message.length(), 500)),
            certificationId
        );
    }

    public record SourceArchive(
        Long materialId,
        String storageKey,
        String originalName,
        String contentType,
        String uid,
        String accountType
    ) {
    }
}

package com.shixianwen.certification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "experience_proof_uploads")
@Getter
@Setter
@NoArgsConstructor
public class ExperienceProofUpload {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "oss_upload_id", nullable = false, length = 256)
    private String ossUploadId;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "expected_size", nullable = false)
    private long expectedSize;

    @Column(name = "part_size", nullable = false)
    private int partSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;
}

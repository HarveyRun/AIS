package com.shixianwen.certification;

import com.shixianwen.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "certifications")
@SQLRestriction("deleted_at IS NULL")
public class Certification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "certification_type", nullable = false, length = 50)
    private String certificationType;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "experience_location", length = 100)
    private String experienceLocation;

    @Column(name = "experience_start_date")
    private String experienceStartDate;

    @Column(name = "experience_end_date")
    private String experienceEndDate;

    @Column(name = "experience_count")
    private Integer experienceCount;

    @Column(name = "experience_role", length = 30)
    private String experienceRole;

    @Column(name = "experience_age_range", length = 20)
    private String experienceAgeRange;

    @Column(name = "experience_education", length = 30)
    private String experienceEducation;

    @Column(name = "experience_job", length = 50)
    private String experienceJob;

    @Column(name = "source_client_platform", nullable = false, length = 20)
    private String sourceClientPlatform = "ANDROID";

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "privacy_confirmed_at")
    private LocalDateTime privacyConfirmedAt;

    @Column(name = "media_processing_status", nullable = false, length = 20)
    private String mediaProcessingStatus = "NOT_REQUIRED";

    @Column(name = "media_processing_error", length = 500)
    private String mediaProcessingError;

    @Column(name = "media_processed_at")
    private LocalDateTime mediaProcessedAt;

    @Column(name = "material_support_score")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer materialSupportScore;

    @Column(name = "common_relevance_score")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer commonRelevanceScore;

    @Column(name = "learnability_score")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer learnabilityScore;

    @Column(name = "clarity_score")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer clarityScore;

    @Column(name = "logic_consistency_score")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer logicConsistencyScore;

    @Column(name = "information_specificity_score")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer informationSpecificityScore;

    @Column(name = "reference_index")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer referenceIndex;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "certification", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<CertificationMaterial> materials = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

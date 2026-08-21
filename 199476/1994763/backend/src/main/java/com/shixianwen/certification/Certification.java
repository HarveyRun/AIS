package com.shixianwen.certification;

import com.shixianwen.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLRestriction;

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

    @Column(name = "discovery_category_id")
    private Long discoveryCategoryId;

    @Column(name = "discovery_experience_id")
    private Long discoveryExperienceId;

    @Column(name = "certification_type", nullable = false, length = 50)
    private String certificationType;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    private Integer years;

    @Column(name = "authenticity_percent")
    private Integer authenticityPercent;

    @Column(name = "job_reapply_available_at")
    private LocalDateTime jobReapplyAvailableAt;

    @Column(name = "required_item", nullable = false)
    private boolean requiredItem;

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

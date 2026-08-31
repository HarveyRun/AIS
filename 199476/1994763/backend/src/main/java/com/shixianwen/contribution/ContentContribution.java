package com.shixianwen.contribution;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "content_contributions")
public class ContentContribution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "matter_name", nullable = false, length = 40)
    private String matterName;

    @Column(name = "raw_payload_encrypted", nullable = false, columnDefinition = "TEXT")
    private String rawPayloadEncrypted;

    @Column(name = "candidate_fingerprint", nullable = false, length = 64, columnDefinition = "char(64)")
    private String candidateFingerprint;

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "standard_matter_name", length = 40)
    private String standardMatterName;

    @Column(name = "standard_matter_key", length = 80)
    private String standardMatterKey;

    @Column(name = "reward_amount", precision = 14, scale = 2)
    private BigDecimal rewardAmount;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    @Column(name = "violation_level", columnDefinition = "tinyint")
    private Integer violationLevel;

    @Column(name = "violation_reason", length = 500)
    private String violationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private AdminUser reviewedByAdmin;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Version
    @Column(name = "version_no", nullable = false)
    private long version;

    @OneToMany(mappedBy = "contribution", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ContentContributionJob> jobs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

package com.shixianwen.quality;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.inquiry.Inquiry;
import com.shixianwen.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inquiry_quality_reviews")
public class InquiryQualityReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false, unique = true)
    private Inquiry inquiry;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questioner_id", nullable = false)
    private User questioner;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answerer_id", nullable = false)
    private User answerer;
    @Column(name = "reason_code", nullable = false, length = 40)
    private String reasonCode;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(nullable = false, length = 30)
    private String status = "PENDING";
    @Column(length = 30)
    private String decision;
    @Column(name = "decision_reason", length = 500)
    private String decisionReason;
    @Column(name = "penalty_duration", length = 30)
    private String penaltyDuration;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private AdminUser reviewedBy;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

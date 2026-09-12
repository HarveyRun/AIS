package com.shixianwen.support;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.user.User;
import com.shixianwen.inquiry.Inquiry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "feedback_records")
public class FeedbackRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id")
    private Inquiry inquiry;
    @Column(name = "feedback_type", nullable = false, length = 30)
    private String feedbackType;
    @Column(nullable = false, length = 80)
    private String category;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(nullable = false, length = 30)
    private String status = "SUBMITTED";
    @Column(length = 1000)
    private String resolution;
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_admin_id")
    private AdminUser handledByAdmin;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

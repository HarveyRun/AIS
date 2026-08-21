package com.shixianwen.invitation;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_invitations")
public class UserInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_user_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_user_id", nullable = false)
    private User invitee;

    @Column(name = "invitation_code", nullable = false, length = 20)
    private String invitationCode;

    @Column(name = "inviter_real_name", nullable = false, length = 30)
    private String inviterRealName;

    @Column(name = "reward_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal rewardAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "review_reason", length = 300)
    private String reviewReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private AdminUser reviewedByAdmin;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rewarded_at")
    private LocalDateTime rewardedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

package com.shixianwen.invitation;

import com.shixianwen.certification.Certification;
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
@Table(name = "experience_invitation_rewards")
public class ExperienceInvitationReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claimant_user_id", nullable = false)
    private User claimant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_user_id", nullable = false)
    private User invitedUser;

    @Column(name = "invitation_code", nullable = false, length = 20)
    private String invitationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_welfare_certification_id")
    private Certification publicWelfareCertification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monetized_certification_id")
    private Certification monetizedCertification;

    @Column(name = "public_welfare_reward_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal publicWelfareRewardAmount;

    @Column(name = "monetized_reward_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal monetizedRewardAmount;

    @Column(name = "total_reward_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRewardAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

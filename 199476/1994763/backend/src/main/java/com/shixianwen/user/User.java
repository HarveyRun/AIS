package com.shixianwen.user;

import com.shixianwen.admin.AdminUser;
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
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String uid;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 40)
    private String nickname;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "capability_description", length = 240)
    private String capabilityDescription;

    @Column(name = "accepting_inquiries", nullable = false)
    private boolean acceptingInquiries = false;

    @Column(name = "accepting_inquiries_updated_at")
    private LocalDateTime acceptingInquiriesUpdatedAt;

    @Column(name = "inquiry_price_min", nullable = false)
    private int inquiryPriceMin = 1;

    @Column(name = "inquiry_price_max", nullable = false)
    private int inquiryPriceMax = 5000;

    @Column(name = "inquiry_price_updated_at")
    private LocalDateTime inquiryPriceUpdatedAt;

    @Column(name = "answerer_status", nullable = false, length = 30)
    private String answererStatus = "NOT_APPLIED";

    @Column(name = "job_certification_blocked_until")
    private LocalDateTime jobCertificationBlockedUntil;

    @Column(name = "account_status", nullable = false, length = 30)
    private String accountStatus = "ACTIVE";

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType = "NORMAL";

    @Column(name = "ban_reason", length = 300)
    private String banReason;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    @Column(name = "ban_until")
    private LocalDateTime banUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by_admin_id")
    private AdminUser bannedByAdmin;

    @Column(name = "register_ip", length = 45, updatable = false)
    private String registerIp;

    @Column(name = "register_location", length = 100, updatable = false)
    private String registerLocation;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "last_login_location", length = 100)
    private String lastLoginLocation;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

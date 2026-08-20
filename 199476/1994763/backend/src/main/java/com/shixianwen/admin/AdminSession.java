package com.shixianwen.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name="admin_sessions")
public class AdminSession {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="admin_user_id") private AdminUser adminUser;
    @Column(name="token_hash", nullable=false, unique=true, length=64) private String tokenHash;
    @Column(name="login_ip", nullable=false, length=45) private String loginIp = "unknown";
    @Column(name="device_id", nullable=false, length=100) private String deviceId = "unknown";
    @Column(name="expires_at", nullable=false) private LocalDateTime expiresAt;
    @CreationTimestamp @Column(name="created_at", updatable=false) private LocalDateTime createdAt;
}

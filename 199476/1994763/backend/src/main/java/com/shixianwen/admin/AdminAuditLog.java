package com.shixianwen.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name="admin_audit_logs")
public class AdminAuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="admin_user_id") private AdminUser adminUser;
    @Column(nullable=false, length=80) private String action;
    @Column(name="target_type", length=50) private String targetType;
    @Column(name="target_id", length=80) private String targetId;
    @Column(length=1000) private String detail;
    @Column(name="ip_address", length=64) private String ipAddress;
    @CreationTimestamp @Column(name="created_at", updatable=false) private LocalDateTime createdAt;
}

package com.shixianwen.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "admin_users")
public class AdminUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=20) private String phone;
    @Column(name="password_hash", nullable=false) private String passwordHash;
    @Column(name="display_name", nullable=false, length=60) private String displayName;
    @Column(nullable=false, length=80) private String role = "SUPER_ADMIN";
    @Column(nullable=false, length=20) private String status = "ACTIVE";
    @Column(name="must_change_password", nullable=false) private boolean mustChangePassword;
    @Column(name="last_login_at") private LocalDateTime lastLoginAt;
    @CreationTimestamp @Column(name="created_at", updatable=false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="deleted_at") private LocalDateTime deletedAt;
}

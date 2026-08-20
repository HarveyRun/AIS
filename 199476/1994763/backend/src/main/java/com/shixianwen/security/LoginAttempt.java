package com.shixianwen.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "login_attempts")
public class LoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;
    @Column(nullable = false, length = 20)
    private String phone;
    @Column(name = "request_ip", nullable = false, length = 45)
    private String requestIp;
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;
    @Column(nullable = false)
    private boolean successful;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

package com.shixianwen.auth;

import com.shixianwen.user.User;
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
@Table(name = "user_login_records")
public class UserLoginRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "ip_location", nullable = false, length = 100)
    private String ipLocation;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId = "unknown";

    @CreationTimestamp
    @Column(name = "logged_in_at", nullable = false, updatable = false)
    private LocalDateTime loggedInAt;
}

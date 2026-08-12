package com.shixianwen.inquiry;

import com.shixianwen.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inquiries")
public class Inquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questioner_id", nullable = false)
    private User questioner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answerer_id", nullable = false)
    private User answerer;

    @Column(length = 120)
    private String topic;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType = "PROFILE";

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(name = "request_ip", length = 45, updatable = false)
    private String requestIp;

    @Column(name = "request_location", length = 100, updatable = false)
    private String requestLocation;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "funds_status", nullable = false, length = 30)
    private String fundsStatus;

    @Column(name = "questioner_unread_count", nullable = false)
    private int questionerUnreadCount;

    @Column(name = "answerer_unread_count", nullable = false)
    private int answererUnreadCount;

    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    @Column(name = "confirmation_deadline")
    private LocalDateTime confirmationDeadline;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;
}

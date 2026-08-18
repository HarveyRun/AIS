package com.shixianwen.support;

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
@Table(name = "customer_service_messages")
public class CustomerServiceMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType;
    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType = "TEXT";
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;
    @Column(name = "attachment_name", length = 255)
    private String attachmentName;
    @Column(name = "attachment_size")
    private Long attachmentSize;
    @Column(name = "read_flag", nullable = false)
    private boolean read;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

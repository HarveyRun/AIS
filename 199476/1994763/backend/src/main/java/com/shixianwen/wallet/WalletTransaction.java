package com.shixianwen.wallet;

import com.shixianwen.user.User;
import jakarta.persistence.*;
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
@Table(name = "wallet_transactions")
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "transaction_type", nullable = false, length = 40)
    private String transactionType;
    @Column(nullable = false, length = 20)
    private String direction;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(name = "available_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal availableAfter;
    @Column(name = "frozen_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal frozenAfter;
    @Column(name = "reference_type", length = 40)
    private String referenceType;
    @Column(name = "reference_id")
    private Long referenceId;
    @Column(nullable = false)
    private String description;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

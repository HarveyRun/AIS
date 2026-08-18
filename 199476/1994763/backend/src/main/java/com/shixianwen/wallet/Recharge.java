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
@Table(name = "recharges")
public class Recharge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;
    @Column(name = "provider_trade_no", length = 100)
    private String providerTradeNo;
    @Column(nullable = false, length = 30)
    private String channel = "ALIPAY";
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

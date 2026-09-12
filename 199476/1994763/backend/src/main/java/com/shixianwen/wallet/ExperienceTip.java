package com.shixianwen.wallet;

import com.shixianwen.certification.Certification;
import com.shixianwen.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "experience_tips")
public class ExperienceTip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_user_id", nullable = false)
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certification_id", nullable = false)
    private Certification certification;

    @Column(name = "request_no", nullable = false, length = 64)
    private String requestNo;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_rate", nullable = false, precision = 7, scale = 6)
    private BigDecimal feeRate = new BigDecimal("0.500000");

    @Column(name = "fee_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal feeAmount = MoneyAmounts.ZERO;

    @Column(name = "receiver_income_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal receiverIncomeAmount = MoneyAmounts.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

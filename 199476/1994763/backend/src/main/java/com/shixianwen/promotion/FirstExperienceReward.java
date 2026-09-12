package com.shixianwen.promotion;

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
@Table(name = "first_experience_rewards")
public class FirstExperienceReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certification_id", nullable = false)
    private Certification certification;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_rate", nullable = false, precision = 7, scale = 6)
    private BigDecimal feeRate = new BigDecimal("0.500000");

    @Column(name = "fee_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "user_income_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal userIncomeAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel = "LOW";

    @Column(name = "risk_reasons", length = 1000)
    private String riskReasons;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

package com.shixianwen.wallet;

import com.shixianwen.inquiry.Inquiry;
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
@Table(name = "platform_fee_records")
public class PlatformFeeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(name = "reference_type", nullable = false, length = 40)
    private String referenceType = "INQUIRY";

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "client_platform", nullable = false, length = 20)
    private String clientPlatform;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "service_fee_rate", nullable = false, precision = 7, scale = 6)
    private BigDecimal serviceFeeRate;

    @Column(name = "service_fee_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal serviceFeeAmount;

    @Column(name = "answerer_income_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal answererIncomeAmount;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

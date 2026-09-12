package com.shixianwen.inquiry;

import com.shixianwen.user.User;
import com.shixianwen.wallet.MoneyAmounts;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "inquiry_voice_calls")
public class InquiryVoiceCall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questioner_id", nullable = false)
    private User questioner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answerer_id", nullable = false)
    private User answerer;

    @Column(name = "max_end_at", nullable = false)
    private LocalDateTime maxEndAt;

    @Column(name = "actual_duration_seconds", nullable = false)
    private int actualDurationSeconds;

    @Column(name = "billable_seconds", nullable = false)
    private int billableSeconds;

    @Column(name = "active_segment_started_at")
    private LocalDateTime activeSegmentStartedAt;

    @Column(name = "hourly_rate_snapshot", nullable = false)
    private int hourlyRateSnapshot;

    @Column(name = "reserved_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal reservedAmount = MoneyAmounts.ZERO;

    @Column(name = "actual_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal actualAmount = MoneyAmounts.ZERO;

    @Column(name = "service_fee_rate", nullable = false, precision = 7, scale = 6)
    private BigDecimal serviceFeeRate = BigDecimal.ZERO.setScale(6);

    @Column(name = "service_fee_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal serviceFeeAmount = MoneyAmounts.ZERO;

    @Column(name = "answerer_income_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal answererIncomeAmount = MoneyAmounts.ZERO;

    @Column(name = "frozen_recharge_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal frozenRechargeAmount = MoneyAmounts.ZERO;

    @Column(name = "frozen_income_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal frozenIncomeAmount = MoneyAmounts.ZERO;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "text_quota_granted", nullable = false)
    private boolean textQuotaGranted;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "connect_deadline")
    private LocalDateTime connectDeadline;

    @Column(name = "questioner_joined_at")
    private LocalDateTime questionerJoinedAt;

    @Column(name = "answerer_joined_at")
    private LocalDateTime answererJoinedAt;

    @Column(name = "questioner_connected_at")
    private LocalDateTime questionerConnectedAt;

    @Column(name = "answerer_connected_at")
    private LocalDateTime answererConnectedAt;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "last_disconnected_at")
    private LocalDateTime lastDisconnectedAt;

    @Column(name = "reconnect_deadline")
    private LocalDateTime reconnectDeadline;

    @Column(name = "end_reason", length = 40)
    private String endReason;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;
}

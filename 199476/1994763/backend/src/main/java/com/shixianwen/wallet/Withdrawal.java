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
@Table(name = "withdrawals")
public class Withdrawal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alipay_account_id")
    private AlipayAccount alipayAccount;
    @Column(name = "request_no", length = 64)
    private String requestNo;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal fee;
    @Column(name = "arrival_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal arrivalAmount;
    @Column(name = "payee_name_snapshot", length = 80)
    private String payeeNameSnapshot;

    @Column(name = "alipay_identifier_type_snapshot", length = 20)
    private String alipayIdentifierTypeSnapshot;
    @Column(name = "alipay_account_ciphertext_snapshot", length = 512)
    private String alipayAccountCiphertextSnapshot;
    @Column(name = "alipay_account_masked_snapshot", length = 120)
    private String alipayAccountMaskedSnapshot;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "batch_no", length = 64)
    private String batchNo;
    @Column(name = "exported_at")
    private LocalDateTime exportedAt;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}

package com.shixianwen.wallet;

import com.shixianwen.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "wallet_accounts")
public class WalletAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "available_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal availableBalance = MoneyAmounts.ZERO;

    @Column(name = "frozen_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal frozenBalance = MoneyAmounts.ZERO;

    @Column(name = "total_withdrawn", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalWithdrawn = MoneyAmounts.ZERO;

    @Version
    private long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

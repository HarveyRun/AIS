package com.shixianwen.wallet;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WithdrawalFeePolicy {
    private static final BigDecimal FREE_LIMIT = new BigDecimal("30000.00");
    private static final BigDecimal RATE = new BigDecimal("0.20");

    public BigDecimal calculate(BigDecimal alreadyWithdrawn, BigDecimal amount) {
        BigDecimal normalizedWithdrawn = MoneyAmounts.normalize(alreadyWithdrawn);
        BigDecimal normalizedAmount = MoneyAmounts.requirePositive(amount);
        BigDecimal freeRemaining = MoneyAmounts.subtract(FREE_LIMIT, normalizedWithdrawn)
            .max(MoneyAmounts.ZERO);
        return MoneyAmounts.normalize(
            MoneyAmounts.subtract(normalizedAmount, freeRemaining)
                .max(MoneyAmounts.ZERO)
                .multiply(RATE)
        );
    }

    public BigDecimal freeLimit() {
        return FREE_LIMIT;
    }

    public BigDecimal rate() {
        return RATE;
    }
}

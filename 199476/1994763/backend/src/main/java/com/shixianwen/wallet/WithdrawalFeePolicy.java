package com.shixianwen.wallet;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class WithdrawalFeePolicy {
    private static final BigDecimal FREE_LIMIT = new BigDecimal("10000.00");
    private static final BigDecimal RATE = new BigDecimal("0.20");

    public BigDecimal calculate(BigDecimal alreadyWithdrawn, BigDecimal amount) {
        BigDecimal freeRemaining = FREE_LIMIT.subtract(alreadyWithdrawn).max(BigDecimal.ZERO);
        return amount.subtract(freeRemaining).max(BigDecimal.ZERO).multiply(RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }
}

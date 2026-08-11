package com.shixianwen.wallet;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WithdrawalFeePolicyTest {
    private final WithdrawalFeePolicy policy = new WithdrawalFeePolicy();

    @Test
    void doesNotChargeWithinFreeLimit() {
        assertThat(policy.calculate(new BigDecimal("3200"), new BigDecimal("800")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void chargesOnlyPartExceedingFreeLimit() {
        assertThat(policy.calculate(new BigDecimal("9500"), new BigDecimal("1000")))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void chargesWholeAmountAfterFreeLimitUsed() {
        assertThat(policy.calculate(new BigDecimal("12000"), new BigDecimal("1000")))
                .isEqualByComparingTo("200.00");
    }
}

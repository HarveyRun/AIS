package com.shixianwen.wallet;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WithdrawalFeePolicyTest {
    private final WithdrawalFeePolicy policy = new WithdrawalFeePolicy();

    @Test
    void doesNotChargeWithinFreeLimit() {
        assertThat(policy.calculate(new BigDecimal("23200"), new BigDecimal("800")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void chargesOnlyPartExceedingFreeLimit() {
        assertThat(policy.calculate(new BigDecimal("29500"), new BigDecimal("1000")))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void chargesWholeAmountAfterFreeLimitUsed() {
        assertThat(policy.calculate(new BigDecimal("32000"), new BigDecimal("1000")))
                .isEqualByComparingTo("200.00");
    }

    @Test
    void exposesCurrentRuleToClients() {
        assertThat(policy.freeLimit()).isEqualByComparingTo("30000.00");
        assertThat(policy.rate()).isEqualByComparingTo("0.20");
    }
}

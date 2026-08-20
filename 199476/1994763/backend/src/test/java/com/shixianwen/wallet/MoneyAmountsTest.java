package com.shixianwen.wallet;

import com.shixianwen.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyAmountsTest {
    @Test
    void normalizesAllInternalMoneyToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("10.24"), MoneyAmounts.normalize(new BigDecimal("10.235")));
        assertEquals(new BigDecimal("10.23"), MoneyAmounts.normalize(new BigDecimal("10.234")));
    }

    @Test
    void externalPaidAmountMustNotContainSubCentPrecision() {
        assertEquals(
            new BigDecimal("10.20"),
            MoneyAmounts.requireExactPositive(new BigDecimal("10.2"), "实付金额")
        );
        assertThrows(
            BusinessException.class,
            () -> MoneyAmounts.requireExactPositive(new BigDecimal("10.201"), "实付金额")
        );
    }

    @Test
    void comparesMoneyWithoutDependingOnScale() {
        assertTrue(MoneyAmounts.same(new BigDecimal("10.00"), new BigDecimal("10.0")));
    }
}

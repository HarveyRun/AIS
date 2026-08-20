package com.shixianwen.wallet;

import com.shixianwen.common.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 平台金额的唯一精度规则。
 *
 * <p>所有入账、扣款、冻结、退款和手续费都必须先经过这里，避免不同业务使用不同精度。</p>
 */
public final class MoneyAmounts {
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private MoneyAmounts() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw BusinessException.badRequest("金额不能为空");
        }
        return amount.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal requirePositive(BigDecimal amount) {
        BigDecimal normalized = normalize(amount);
        if (normalized.compareTo(ZERO) <= 0) {
            throw BusinessException.badRequest("金额必须大于0");
        }
        return normalized;
    }

    public static BigDecimal requireExactPositive(BigDecimal amount, String label) {
        if (amount == null) {
            throw BusinessException.badRequest(label + "不能为空");
        }
        final BigDecimal normalized;
        try {
            normalized = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw BusinessException.badRequest(label + "最多支持两位小数");
        }
        if (normalized.compareTo(ZERO) <= 0) {
            throw BusinessException.badRequest(label + "必须大于0");
        }
        return normalized;
    }

    public static BigDecimal requireWholeAmount(
        BigDecimal amount,
        BigDecimal minimum,
        BigDecimal maximum,
        String label
    ) {
        if (amount == null) {
            throw BusinessException.badRequest("请输入" + label);
        }
        try {
            amount.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw BusinessException.badRequest(label + "只支持整数");
        }
        if (amount.compareTo(minimum) < 0 || amount.compareTo(maximum) > 0) {
            throw BusinessException.badRequest(
                label + "必须在" + minimum.toPlainString() + "至" + maximum.toPlainString() + "之间"
            );
        }
        return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal add(BigDecimal left, BigDecimal right) {
        return normalize(normalize(left).add(normalize(right)));
    }

    public static BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return normalize(normalize(left).subtract(normalize(right)));
    }

    public static boolean same(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }
}

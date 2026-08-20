package com.shixianwen.wallet;

import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PlatformServiceFeePolicy {
    public static final String ANDROID = "ANDROID";
    public static final String IOS = "IOS";

    private final PlatformFeeSettingRepository settings;

    @Transactional(readOnly = true)
    public SettlementQuote quote(BigDecimal grossAmount, String clientPlatform) {
        BigDecimal gross = MoneyAmounts.requirePositive(grossAmount);
        String platform = normalizePlatform(clientPlatform);
        BigDecimal rate = currentRate(platform);
        BigDecimal fee = MoneyAmounts.normalize(gross.multiply(rate));
        return new SettlementQuote(platform, gross, rate, fee, MoneyAmounts.subtract(gross, fee));
    }

    @Transactional(readOnly = true)
    public BigDecimal currentRate(String clientPlatform) {
        String platform = normalizePlatform(clientPlatform);
        return settings.findByClientPlatform(platform)
            .map(PlatformFeeSetting::getServiceFeeRate)
            .map(this::normalizeRate)
            .orElseThrow(() -> BusinessException.badRequest(platform + "平台服务费配置不存在"));
    }

    public String normalizePlatform(String clientPlatform) {
        return IOS.equalsIgnoreCase(clientPlatform == null ? "" : clientPlatform.trim()) ? IOS : ANDROID;
    }

    public BigDecimal requireValidRate(BigDecimal rate) {
        if (rate == null) throw BusinessException.badRequest("请输入平台服务费率");
        BigDecimal normalized = normalizeRate(rate);
        if (normalized.compareTo(BigDecimal.ZERO) < 0
            || normalized.compareTo(new BigDecimal("0.990000")) > 0) {
            throw BusinessException.badRequest("平台服务费率必须在0%至99%之间");
        }
        return normalized;
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        return rate.setScale(6, RoundingMode.HALF_UP);
    }

    public record SettlementQuote(
        String clientPlatform,
        BigDecimal grossAmount,
        BigDecimal serviceFeeRate,
        BigDecimal serviceFeeAmount,
        BigDecimal answererIncomeAmount
    ) {
    }
}

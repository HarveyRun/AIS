package com.shixianwen.wallet;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformServiceFeePolicyTest {
    @Test
    void calculatesAndroidIncomeUsingAndroidRate() {
        PlatformFeeSettingRepository settings = mock(PlatformFeeSettingRepository.class);
        when(settings.findByClientPlatform("ANDROID"))
            .thenReturn(Optional.of(setting("ANDROID", "0.050000")));

        PlatformServiceFeePolicy.SettlementQuote quote =
            new PlatformServiceFeePolicy(settings).quote(new BigDecimal("100"), "android");

        assertThat(quote.clientPlatform()).isEqualTo("ANDROID");
        assertThat(quote.serviceFeeAmount()).isEqualByComparingTo("5.00");
        assertThat(quote.answererIncomeAmount()).isEqualByComparingTo("95.00");
    }

    @Test
    void calculatesIosIncomeUsingIosRateAndRoundsToCents() {
        PlatformFeeSettingRepository settings = mock(PlatformFeeSettingRepository.class);
        when(settings.findByClientPlatform("IOS"))
            .thenReturn(Optional.of(setting("IOS", "0.055000")));

        PlatformServiceFeePolicy.SettlementQuote quote =
            new PlatformServiceFeePolicy(settings).quote(new BigDecimal("99"), "ios");

        assertThat(quote.clientPlatform()).isEqualTo("IOS");
        assertThat(quote.serviceFeeAmount()).isEqualByComparingTo("5.45");
        assertThat(quote.answererIncomeAmount()).isEqualByComparingTo("93.55");
    }

    @Test
    void unknownClientFallsBackToAndroid() {
        PlatformFeeSettingRepository settings = mock(PlatformFeeSettingRepository.class);
        when(settings.findByClientPlatform("ANDROID"))
            .thenReturn(Optional.of(setting("ANDROID", "0.050000")));

        PlatformServiceFeePolicy.SettlementQuote quote =
            new PlatformServiceFeePolicy(settings).quote(new BigDecimal("100"), "web");

        assertThat(quote.clientPlatform()).isEqualTo("ANDROID");
    }

    @Test
    void rejectsAFullDeductionRate() {
        PlatformFeeSettingRepository settings = mock(PlatformFeeSettingRepository.class);
        PlatformServiceFeePolicy policy = new PlatformServiceFeePolicy(settings);

        assertThatThrownBy(() -> policy.requireValidRate(BigDecimal.ONE))
            .hasMessageContaining("0%至99%");
    }

    private PlatformFeeSetting setting(String platform, String rate) {
        PlatformFeeSetting setting = new PlatformFeeSetting();
        setting.setClientPlatform(platform);
        setting.setServiceFeeRate(new BigDecimal(rate));
        return setting;
    }
}

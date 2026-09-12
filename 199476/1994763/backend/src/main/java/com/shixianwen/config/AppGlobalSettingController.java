package com.shixianwen.config;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import com.shixianwen.wallet.PlatformServiceFeePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app-settings")
@RequiredArgsConstructor
public class AppGlobalSettingController {
    private final AppGlobalSettingService service;
    private final PlatformServiceFeePolicy platformFees;

    @GetMapping
    public ApiResponse<AppSettingsView> current(@CurrentUser User user) {
        return ApiResponse.ok(new AppSettingsView(
            service.current(),
            platformFees.currentRate("ANDROID"),
            platformFees.currentRate("IOS")
        ));
    }

    public record AppSettingsView(
        AppGlobalSettingService.Settings rules,
        java.math.BigDecimal androidLaborFeeRate,
        java.math.BigDecimal iosLaborFeeRate
    ) {}
}

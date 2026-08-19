package com.shixianwen.version;

import com.shixianwen.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/app-version")
@RequiredArgsConstructor
public class PublicAppVersionController {
    private final AppVersionService service;

    @GetMapping
    public ApiResponse<AppVersionService.UpdateCheck> check(
        @RequestParam(defaultValue = "ANDROID") String platform,
        @RequestParam int currentVersionCode
    ) {
        return ApiResponse.ok(service.check(platform, currentVersionCode));
    }
}

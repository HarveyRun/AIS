package com.shixianwen.analytics;

import com.shixianwen.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {
    private final AdminAnalyticsService analytics;

    @GetMapping("/{section:overview|funnel|content|supply|answerers|retention|invitation|quality}")
    public ApiResponse<Map<String, Object>> section(
        @PathVariable String section,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "") String platform,
        @RequestParam(defaultValue = "prod") String environment,
        @RequestParam(defaultValue = "false") boolean includeTest
    ) {
        return ApiResponse.ok(analytics.section(
            section,
            new AdminAnalyticsService.Query(startDate, endDate, platform, environment, includeTest)
        ));
    }

    @GetMapping("/raw")
    public ApiResponse<AdminAnalyticsService.PageResult> raw(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "") String platform,
        @RequestParam(defaultValue = "prod") String environment,
        @RequestParam(defaultValue = "false") boolean includeTest,
        @RequestParam(defaultValue = "") String eventName,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(analytics.raw(
            new AdminAnalyticsService.Query(startDate, endDate, platform, environment, includeTest),
            eventName,
            page,
            size
        ));
    }
}

package com.shixianwen.analytics;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsEventService analytics;

    @PostMapping("/events")
    public ApiResponse<AnalyticsEventService.IngestResult> ingest(
        @CurrentUser User user,
        @RequestBody AnalyticsEventService.ClientBatch batch
    ) {
        return ApiResponse.ok(analytics.ingest(user, batch));
    }
}

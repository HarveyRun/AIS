package com.shixianwen.quality;

import com.shixianwen.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/answer-quality")
@RequiredArgsConstructor
public class AdminInquiryQualityController {
    private final AdminInquiryQualityService service;

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(service.summary());
    }

    @GetMapping("/evaluations")
    public ApiResponse<AdminInquiryQualityService.PageResult> evaluations(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String risk,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.evaluations(
            keyword,
            risk,
            Math.max(page, 0),
            Math.max(1, Math.min(size, 100))
        ));
    }
}

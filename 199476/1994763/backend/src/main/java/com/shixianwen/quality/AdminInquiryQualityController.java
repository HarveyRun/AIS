package com.shixianwen.quality;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/answer-quality")
@RequiredArgsConstructor
public class AdminInquiryQualityController {
    private final AdminInquiryQualityService service;

    @GetMapping
    public ApiResponse<AdminInquiryQualityService.PageResult> list(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.list(keyword, status, Math.max(page, 0), Math.max(1, Math.min(size, 100))));
    }

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
            keyword, risk, Math.max(page, 0), Math.max(1, Math.min(size, 100))
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @GetMapping("/{id}/evidence")
    public ApiResponse<List<Map<String, Object>>> evidence(@PathVariable Long id) {
        return ApiResponse.ok(service.evidence(id));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<Map<String, Object>> resolve(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @RequestBody ResolveRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.resolve(
            admin, id, request.decision(), request.reason(), request.penaltyDuration(), ip(servletRequest)
        ));
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    public record ResolveRequest(String decision, String reason, String penaltyDuration) {
    }
}

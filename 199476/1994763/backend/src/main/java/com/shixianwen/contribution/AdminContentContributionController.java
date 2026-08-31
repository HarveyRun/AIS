package com.shixianwen.contribution;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/content-contributions")
@RequiredArgsConstructor
public class AdminContentContributionController {
    private final ContentContributionService service;

    @GetMapping
    public ApiResponse<ContentContributionService.PageResult> list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.adminList(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContentContributionService.ContributionView> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.adminDetail(id));
    }

    @GetMapping("/{id}/original")
    public ApiResponse<ContentContributionService.RawPayloadView> original(@PathVariable Long id) {
        return ApiResponse.ok(service.original(id));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<ContentContributionService.ContributionView> review(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @RequestBody ContentContributionService.ReviewCommand command,
        HttpServletRequest request
    ) {
        if ("VIOLATION_REJECTED".equalsIgnoreCase(command.decision())) {
            throw BusinessException.forbidden("当前操作需要内容共建违规处理权限");
        }
        return ApiResponse.ok(service.review(admin, id, command, clientIp(request)));
    }

    @PostMapping("/{id}/violation-review")
    public ApiResponse<ContentContributionService.ContributionView> violationReview(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @RequestBody ContentContributionService.ReviewCommand command,
        HttpServletRequest request
    ) {
        if (!"VIOLATION_REJECTED".equalsIgnoreCase(command.decision())) {
            throw BusinessException.badRequest("违规处理结果不正确");
        }
        return ApiResponse.ok(service.review(admin, id, command, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}

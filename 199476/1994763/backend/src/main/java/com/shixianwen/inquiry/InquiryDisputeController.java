package com.shixianwen.inquiry;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.network.ClientNetworkService;
import com.shixianwen.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InquiryDisputeController {
    private final InquiryDisputeService service;
    private final ClientNetworkService clientNetworkService;

    @PostMapping("/api/inquiries/{inquiryId}/messages/{messageId}/reports")
    public ApiResponse<InquiryDisputeService.MessageReportView> report(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long messageId,
        @Valid @RequestBody ReportRequest request
    ) {
        return ApiResponse.ok(service.report(user.getId(), inquiryId, messageId, request.reportType()));
    }

    @GetMapping("/api/inquiries/{inquiryId}/messages/{messageId}/reports/status")
    public ApiResponse<InquiryDisputeService.MessageReportStatus> reportStatus(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long messageId
    ) {
        return ApiResponse.ok(service.reportStatus(user.getId(), inquiryId, messageId));
    }

    @GetMapping("/api/admin/inquiry-disputes/end-requests")
    public ApiResponse<InquiryDisputeService.PageResult> endDisputes(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.endDisputes(keyword, status, safePage(page), safeSize(size)));
    }

    @PutMapping("/api/admin/inquiry-disputes/end-requests/{id}")
    public ApiResponse<Void> resolveEndDispute(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @Valid @RequestBody EndDecisionRequest request,
        HttpServletRequest servletRequest
    ) {
        service.resolveEndDispute(
            admin,
            id,
            request.decision(),
            request.reason(),
            ip(servletRequest)
        );
        return ApiResponse.ok();
    }

    @GetMapping("/api/admin/inquiry-disputes/message-reports")
    public ApiResponse<InquiryDisputeService.PageResult> reportCases(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.reportCases(keyword, status, safePage(page), safeSize(size)));
    }

    @GetMapping("/api/admin/inquiry-disputes/message-reports/{id}")
    public ApiResponse<Map<String, Object>> reportCase(@PathVariable Long id) {
        return ApiResponse.ok(service.reportCaseDetail(id));
    }

    @PutMapping("/api/admin/inquiry-disputes/message-reports/{id}")
    public ApiResponse<Void> resolveReportCase(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @Valid @RequestBody ReportCaseDecisionRequest request,
        HttpServletRequest servletRequest
    ) {
        service.resolveReportCase(admin, id, request.decisions(), ip(servletRequest));
        return ApiResponse.ok();
    }

    @GetMapping("/api/admin/inquiry-disputes/message-reports/details/{reportId}/original")
    public ApiResponse<Map<String, Object>> originalEvidence(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long reportId,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.originalEvidence(admin, reportId, ip(servletRequest)));
    }

    @GetMapping("/api/admin/inquiry-disputes/risk-watch")
    public ApiResponse<InquiryDisputeService.PageResult> riskWatch(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.riskWatch(keyword, status, safePage(page), safeSize(size)));
    }

    @PutMapping("/api/admin/inquiry-disputes/risk-watch/{userId}")
    public ApiResponse<Void> dismissRisk(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long userId,
        @Valid @RequestBody RiskDecisionRequest request,
        HttpServletRequest servletRequest
    ) {
        service.dismissRisk(admin, userId, request.reason(), ip(servletRequest));
        return ApiResponse.ok();
    }

    private int safePage(int page) {
        return Math.max(0, page);
    }

    private int safeSize(int size) {
        return Math.max(1, Math.min(100, size));
    }

    private String ip(HttpServletRequest request) {
        return clientNetworkService.resolve(request).ipAddress();
    }

    public record ReportRequest(@NotBlank String reportType) {
    }

    public record EndDecisionRequest(@NotBlank String decision, @NotBlank String reason) {
    }

    public record ReportCaseDecisionRequest(
        @NotEmpty List<InquiryDisputeService.ReportDecision> decisions
    ) {
    }

    public record RiskDecisionRequest(@NotBlank String reason) {
    }
}

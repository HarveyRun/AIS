package com.shixianwen.inquiry;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.network.ClientNetworkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InquiryDisputeController {
    private final InquiryDisputeService service;
    private final ClientNetworkService clientNetworkService;

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
        service.dismissRisk(
            admin,
            userId,
            request.reason(),
            clientNetworkService.resolve(servletRequest).ipAddress()
        );
        return ApiResponse.ok();
    }

    private int safePage(int page) {
        return Math.max(0, page);
    }

    private int safeSize(int size) {
        return Math.max(1, Math.min(100, size));
    }

    public record RiskDecisionRequest(@NotBlank String reason) {
    }
}

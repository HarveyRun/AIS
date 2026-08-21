package com.shixianwen.invitation;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/invitation-campaign")
public class AdminInvitationCampaignController {
    private final InvitationCampaignService service;

    public AdminInvitationCampaignController(InvitationCampaignService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<InvitationCampaignService.AdminView> get() {
        return ApiResponse.ok(service.adminView());
    }

    @PutMapping
    public ApiResponse<InvitationCampaignService.AdminView> update(
        @CurrentAdmin AdminUser admin,
        @Valid @RequestBody SaveRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.update(
            admin,
            request.enabled(),
            request.rewardAmount(),
            clientIp(servletRequest)
        ));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    public record SaveRequest(
        boolean enabled,
        @DecimalMin(value = "0.01", message = "红包金额最低0.01元")
        @DecimalMax(value = "999.00", message = "红包金额不能超过999元")
        @Digits(integer = 3, fraction = 2, message = "红包金额最多支持两位小数")
        BigDecimal rewardAmount
    ) {
    }
}

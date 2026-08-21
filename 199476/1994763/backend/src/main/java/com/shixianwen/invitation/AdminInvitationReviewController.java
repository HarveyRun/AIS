package com.shixianwen.invitation;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.admin.CurrentAdmin;
import com.shixianwen.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/invitations")
public class AdminInvitationReviewController {
    private final InvitationCampaignService service;

    public AdminInvitationReviewController(InvitationCampaignService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<InvitationCampaignService.ReviewPage> list(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.reviewPage(keyword, status, page, size));
    }

    @GetMapping("/{id}/identity-materials")
    public ApiResponse<List<InvitationCampaignService.MaterialView>> identityMaterials(
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.inviterIdentityMaterials(id));
    }

    @GetMapping("/{id}/invitee-handheld-material")
    public ApiResponse<InvitationCampaignService.MaterialView> inviteeHandheldMaterial(
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.inviteeHandheldIdentityMaterial(id));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<InvitationCampaignService.ReviewItem> review(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @RequestBody ReviewRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.review(
            admin,
            id,
            request.approved(),
            request.reason(),
            clientIp(servletRequest)
        ));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    public record ReviewRequest(boolean approved, String reason) {
    }
}

package com.shixianwen.invitation;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
    private final InvitationCampaignService service;

    public InvitationController(InvitationCampaignService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ApiResponse<InvitationCampaignService.UserStatusView> status(@CurrentUser User user) {
        return ApiResponse.ok(service.userStatus(user));
    }

    @PostMapping("/bind")
    public ApiResponse<InvitationCampaignService.UserStatusView> bind(
        @CurrentUser User user,
        @Valid @RequestBody BindRequest request
    ) {
        return ApiResponse.ok(service.bind(
            user,
            request.invitationCode(),
            request.inviterRealName()
        ));
    }

    public record BindRequest(
        @NotBlank(message = "请输入邀请码")
        @Pattern(regexp = "^\\d{7}$", message = "请输入对方的7位UID")
        String invitationCode,
        @NotBlank(message = "请输入对方的真实姓名")
        String inviterRealName
    ) {
    }
}

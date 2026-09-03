package com.shixianwen.invitation;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.network.ClientIpExtractor;
import com.shixianwen.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experience-invitation-rewards")
@RequiredArgsConstructor
public class ExperienceInvitationRewardController {
    private final ExperienceInvitationRewardService service;
    private final ClientIpExtractor clientIpExtractor;

    @PostMapping("/redeem")
    public ApiResponse<ExperienceInvitationRewardService.RewardView> redeem(
        @CurrentUser User user,
        @Valid @RequestBody RedeemRequest request,
        HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(service.redeem(
            user,
            request.invitedUid(),
            request.invitedPhone(),
            clientIpExtractor.extract(httpRequest),
            httpRequest.getHeader("X-Device-Id")
        ));
    }

    public record RedeemRequest(
        @NotBlank(message = "请输入对方UID")
        @Pattern(regexp = "^\\d{7}$", message = "请输入对方的7位UID")
        String invitedUid,
        @NotBlank(message = "请输入对方的注册手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
        String invitedPhone
    ) {
    }
}

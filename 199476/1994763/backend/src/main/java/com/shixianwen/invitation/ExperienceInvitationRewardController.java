package com.shixianwen.invitation;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.network.ClientIpExtractor;
import com.shixianwen.user.User;
import jakarta.servlet.http.HttpServletRequest;
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
        @RequestBody RedeemRequest request,
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

    public record RedeemRequest(String invitedUid, String invitedPhone) {
    }
}

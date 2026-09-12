package com.shixianwen.inquiry;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inquiries/{inquiryId}/voice-calls")
@RequiredArgsConstructor
public class InquiryVoiceCallController {
    private final InquiryVoiceCallService service;

    @GetMapping("/latest")
    public ApiResponse<InquiryVoiceCallService.VoiceCallView> latest(
        @CurrentUser User user,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.ok(service.latest(user.getId(), inquiryId));
    }

    @PostMapping
    public ApiResponse<InquiryVoiceCallService.VoiceCallView> create(
        @CurrentUser User user,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.ok(service.create(user.getId(), inquiryId));
    }

    @PostMapping("/{voiceCallId}/answer")
    public ApiResponse<InquiryVoiceCallService.VoiceCallView> answer(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long voiceCallId
    ) {
        return ApiResponse.ok(service.answer(user.getId(), inquiryId, voiceCallId));
    }

    @PostMapping("/{voiceCallId}/reject")
    public ApiResponse<InquiryVoiceCallService.VoiceCallView> reject(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long voiceCallId
    ) {
        return ApiResponse.ok(service.reject(user.getId(), inquiryId, voiceCallId));
    }

    @PostMapping("/{voiceCallId}/join")
    public ApiResponse<InquiryVoiceCallService.VoiceCallView> join(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long voiceCallId
    ) {
        return ApiResponse.ok(service.joinCall(user.getId(), inquiryId, voiceCallId));
    }

    @PostMapping("/{voiceCallId}/connected")
    public ApiResponse<InquiryVoiceCallService.VoiceCallView> connected(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long voiceCallId
    ) {
        return ApiResponse.ok(service.connected(user.getId(), inquiryId, voiceCallId));
    }

    @PostMapping("/{voiceCallId}/disconnected")
    public ApiResponse<InquiryVoiceCallService.VoiceCallView> disconnected(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long voiceCallId,
        @RequestBody(required = false) DisconnectRequest request
    ) {
        return ApiResponse.ok(service.disconnected(
            user.getId(), inquiryId, voiceCallId, request == null ? null : request.reason()
        ));
    }

    @PostMapping("/{voiceCallId}/finish")
    public ApiResponse<InquiryVoiceCallService.VoiceCallView> finish(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long voiceCallId
    ) {
        return ApiResponse.ok(service.finishCall(user.getId(), inquiryId, voiceCallId));
    }

    public record DisconnectRequest(String reason) {}
}

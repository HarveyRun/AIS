package com.shixianwen.inquiry;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries/{inquiryId}/audio-appointments/{appointmentId}/voice")
@RequiredArgsConstructor
public class InquiryVoiceSignalController {
    private final InquiryVoiceSignalService service;

    @GetMapping("/ice-config")
    public ApiResponse<InquiryVoiceSignalService.IceConfig> iceConfig(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long appointmentId
    ) {
        return ApiResponse.ok(service.iceConfig(user.getId(), inquiryId, appointmentId));
    }

    @GetMapping("/signals")
    public ApiResponse<List<InquiryVoiceSignalService.SignalView>> signals(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long appointmentId,
        @RequestParam(defaultValue = "0") long afterId
    ) {
        return ApiResponse.ok(service.pending(user.getId(), inquiryId, appointmentId, afterId));
    }

    @PostMapping("/signals")
    public ApiResponse<InquiryVoiceSignalService.SignalView> signal(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long appointmentId,
        @Valid @RequestBody SignalRequest request
    ) {
        return ApiResponse.ok(service.send(
            user.getId(), inquiryId, appointmentId, request.signalType(), request.payload()
        ));
    }

    public record SignalRequest(@NotBlank String signalType, @NotBlank String payload) {}
}

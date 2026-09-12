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
@RequestMapping("/api/inquiries/{inquiryId}/audio-appointments")
@RequiredArgsConstructor
public class InquiryAudioAppointmentController {
    private final InquiryAudioAppointmentService service;

    @GetMapping("/latest")
    public ApiResponse<InquiryAudioAppointmentService.AppointmentView> latest(
        @CurrentUser User user,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.ok(service.latest(user.getId(), inquiryId));
    }

    @PostMapping
    public ApiResponse<InquiryAudioAppointmentService.AppointmentView> create(
        @CurrentUser User user,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.ok(service.create(user.getId(), inquiryId));
    }

    @PostMapping("/{appointmentId}/answer")
    public ApiResponse<InquiryAudioAppointmentService.AppointmentView> answer(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long appointmentId
    ) {
        return ApiResponse.ok(service.answer(user.getId(), inquiryId, appointmentId));
    }

    @PostMapping("/{appointmentId}/reject")
    public ApiResponse<InquiryAudioAppointmentService.AppointmentView> reject(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long appointmentId
    ) {
        return ApiResponse.ok(service.reject(user.getId(), inquiryId, appointmentId));
    }

    @PostMapping("/{appointmentId}/join")
    public ApiResponse<InquiryAudioAppointmentService.AppointmentView> join(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long appointmentId
    ) {
        return ApiResponse.ok(service.joinCall(user.getId(), inquiryId, appointmentId));
    }

    @PostMapping("/{appointmentId}/connected")
    public ApiResponse<InquiryAudioAppointmentService.AppointmentView> connected(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long appointmentId
    ) {
        return ApiResponse.ok(service.connected(user.getId(), inquiryId, appointmentId));
    }

    @PostMapping("/{appointmentId}/disconnected")
    public ApiResponse<InquiryAudioAppointmentService.AppointmentView> disconnected(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long appointmentId,
        @RequestBody(required = false) DisconnectRequest request
    ) {
        return ApiResponse.ok(service.disconnected(
            user.getId(), inquiryId, appointmentId, request == null ? null : request.reason()
        ));
    }

    @PostMapping("/{appointmentId}/finish")
    public ApiResponse<InquiryAudioAppointmentService.AppointmentView> finish(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @PathVariable Long appointmentId
    ) {
        return ApiResponse.ok(service.finishCall(user.getId(), inquiryId, appointmentId));
    }

    public record DisconnectRequest(String reason) {}
}

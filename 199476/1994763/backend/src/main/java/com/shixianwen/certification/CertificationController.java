package com.shixianwen.certification;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {
    private final CertificationService certificationService;
    private final JobCertificationAppointmentService appointmentService;

    public CertificationController(
        CertificationService certificationService,
        JobCertificationAppointmentService appointmentService
    ) {
        this.certificationService = certificationService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/me")
    public ApiResponse<List<CertificationService.CertificationView>> list(@CurrentUser User user) {
        return ApiResponse.ok(certificationService.list(user));
    }

    @PostMapping(value = "/basic/{type}", consumes = "multipart/form-data")
    public ApiResponse<CertificationService.CertificationView> submitBasic(
        @CurrentUser User user,
        @PathVariable String type,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) Integer years,
        @RequestPart("files") List<MultipartFile> files
    ) {
        return ApiResponse.ok(certificationService.submitBasic(user, type, title, years, files));
    }

    @PostMapping(value = "/experiences", consumes = "multipart/form-data")
    public ApiResponse<CertificationService.CertificationView> submitExperience(
        @CurrentUser User user,
        @RequestParam(required = false) Long existingId,
        @RequestParam String title,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Integer years,
        @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.ok(certificationService.submitExperience(
            user, existingId, title, description, years, files == null ? List.of() : files
        ));
    }

    @GetMapping("/job/offline-appointment")
    public ApiResponse<JobCertificationAppointmentService.AppointmentView> currentOfflineAppointment(
        @CurrentUser User user
    ) {
        return ApiResponse.ok(appointmentService.current(user));
    }

    @PostMapping("/job/offline-appointment")
    public ApiResponse<JobCertificationAppointmentService.AppointmentView> bookOfflineAppointment(
        @CurrentUser User user,
        @RequestBody OfflineAppointmentRequest request
    ) {
        return ApiResponse.ok(appointmentService.book(user, request.appointmentAt()));
    }

    @GetMapping("/job/offline-appointment/availability")
    public ApiResponse<JobCertificationAppointmentService.AvailabilityView> offlineAppointmentAvailability(
        @CurrentUser User user,
        @RequestParam LocalDateTime appointmentAt
    ) {
        return ApiResponse.ok(appointmentService.availability(appointmentAt));
    }

    public record OfflineAppointmentRequest(LocalDateTime appointmentAt) {
    }

}

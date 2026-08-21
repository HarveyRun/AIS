package com.shixianwen.admin;

import com.shixianwen.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/job-certification-appointments")
@RequiredArgsConstructor
public class AdminJobCertificationAppointmentController {
    private final AdminJobCertificationAppointmentService service;

    @GetMapping
    public ApiResponse<AdminJobCertificationAppointmentService.PageResult> list(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(
            service.list(keyword, status, Math.max(page, 0), Math.max(1, Math.min(size, 100)))
        );
    }

    @GetMapping("/{id}/materials")
    public ApiResponse<List<Map<String, Object>>> materials(@PathVariable Long id) {
        return ApiResponse.ok(service.materials(id));
    }

    @PostMapping(path = "/{id}/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> process(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @RequestParam String status,
        @RequestParam(defaultValue = "") String reason,
        @RequestParam(required = false) Long jobId,
        @RequestParam(required = false) Integer years,
        @RequestParam(required = false) Integer authenticityPercent,
        @RequestPart(value = "evidence", required = false) MultipartFile evidence,
        HttpServletRequest request
    ) {
        service.process(
            admin,id,status,reason,jobId,years,authenticityPercent,evidence,ip(request)
        );
        return ApiResponse.ok();
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}

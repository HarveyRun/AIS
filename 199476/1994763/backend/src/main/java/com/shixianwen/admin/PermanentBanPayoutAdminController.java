package com.shixianwen.admin;

import com.shixianwen.common.ApiResponse;
import com.shixianwen.wallet.PermanentBanPayoutService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/permanent-ban-payouts")
@RequiredArgsConstructor
public class PermanentBanPayoutAdminController {
    private final PermanentBanPayoutService service;

    @GetMapping
    public ApiResponse<AdminManagementService.PageResult> page(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.page(
            keyword,
            status,
            Math.max(page, 0),
            Math.max(1, Math.min(size, 100))
        ));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(
        @CurrentAdmin AdminUser admin,
        HttpServletRequest request
    ) {
        return file(service.export(admin, ip(request)));
    }

    @GetMapping("/export/{batchNo}")
    public ResponseEntity<byte[]> download(@PathVariable String batchNo) {
        return file(service.downloadBatch(batchNo));
    }

    @PostMapping("/results")
    public ApiResponse<Map<String, Object>> importResults(
        @CurrentAdmin AdminUser admin,
        @RequestPart MultipartFile file,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(service.importResults(admin, file, ip(request)));
    }

    @PatchMapping("/{id}/retry")
    public ApiResponse<Void> retry(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        service.retry(admin, id, ip(request));
        return ApiResponse.ok();
    }

    private ResponseEntity<byte[]> file(PermanentBanPayoutService.ExportFile file) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + file.encodedFilename()
            )
            .header("X-Export-Count", String.valueOf(file.count()))
            .body(file.content());
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}

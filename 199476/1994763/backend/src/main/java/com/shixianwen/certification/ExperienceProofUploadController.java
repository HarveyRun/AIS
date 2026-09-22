package com.shixianwen.certification;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certifications/experience-uploads")
public class ExperienceProofUploadController {
    private final ExperienceProofUploadService uploads;

    public ExperienceProofUploadController(ExperienceProofUploadService uploads) {
        this.uploads = uploads;
    }

    @GetMapping("/capabilities")
    public ApiResponse<Capabilities> capabilities(@CurrentUser User user) {
        return ApiResponse.ok(new Capabilities(uploads.directAvailable()));
    }

    @PostMapping
    public ApiResponse<ExperienceProofUploadService.UploadView> initiate(
        @CurrentUser User user, @RequestBody InitiateRequest request
    ) {
        return ApiResponse.ok(uploads.initiate(user, request.name(), request.size()));
    }

    @PostMapping("/{id}/parts/{partNumber}")
    public ApiResponse<ExperienceProofUploadService.PartUrl> signPart(
        @CurrentUser User user, @PathVariable String id, @PathVariable int partNumber
    ) {
        return ApiResponse.ok(uploads.signPart(user, id, partNumber));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<ExperienceProofUploadService.UploadView> complete(
        @CurrentUser User user, @PathVariable String id
    ) {
        return ApiResponse.ok(uploads.complete(user, id));
    }

    @PostMapping("/{id}/abort")
    public ApiResponse<Void> abort(@CurrentUser User user, @PathVariable String id) {
        uploads.abort(user, id);
        return ApiResponse.ok();
    }

    public record InitiateRequest(String name, long size) {}
    public record Capabilities(boolean directUploadAvailable) {}
}

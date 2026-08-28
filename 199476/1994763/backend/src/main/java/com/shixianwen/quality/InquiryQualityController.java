package com.shixianwen.quality;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries/{inquiryId}/quality")
@RequiredArgsConstructor
public class InquiryQualityController {
    private final InquiryQualityService service;

    @GetMapping
    public ApiResponse<InquiryQualityService.QualityOptions> options(
        @CurrentUser User user,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.ok(service.options(user.getId(), inquiryId));
    }

    @PostMapping("/evaluation")
    public ApiResponse<InquiryQualityService.EvaluationView> evaluate(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @Valid @RequestBody EvaluationRequest request
    ) {
        return ApiResponse.ok(service.evaluate(
            user.getId(), inquiryId,
            new InquiryQualityService.EvaluationCommand(
                request.answeredLevel(), request.specificLevel(), request.matchedLevel(),
                request.usefulLevel(), request.communicationLevel(), request.askAgainLevel(),
                request.negativeTags(), request.comment()
            )
        ));
    }

    public record EvaluationRequest(
        @NotNull Integer answeredLevel,
        @NotNull Integer specificLevel,
        @NotNull Integer matchedLevel,
        @NotNull Integer usefulLevel,
        @NotNull Integer communicationLevel,
        @NotNull Integer askAgainLevel,
        @Size(max = 8) List<String> negativeTags,
        @Size(max = 300) String comment
    ) {
    }

}

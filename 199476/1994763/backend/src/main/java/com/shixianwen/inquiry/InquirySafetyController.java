package com.shixianwen.inquiry;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.support.SupportService;
import com.shixianwen.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inquiries/{inquiryId}")
@RequiredArgsConstructor
public class InquirySafetyController {
    private final SupportService supportService;
    private final UserCommunicationBlockService blockService;

    @GetMapping("/complaints/status")
    public ApiResponse<ComplaintStatus> complaintStatus(
        @CurrentUser User user,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.ok(new ComplaintStatus(
            supportService.hasInquiryComplaint(user.getId(), inquiryId)
        ));
    }

    @PostMapping("/complaints")
    public ApiResponse<SupportService.FeedbackView> complain(
        @CurrentUser User user,
        @PathVariable Long inquiryId,
        @Valid @RequestBody ComplaintRequest request
    ) {
        return ApiResponse.ok(supportService.inquiryComplaint(
            user.getId(),
            inquiryId,
            request.category(),
            request.content()
        ));
    }

    @PostMapping("/block")
    public ApiResponse<UserCommunicationBlockService.BlockView> block(
        @CurrentUser User user,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.ok(blockService.block(user.getId(), inquiryId));
    }

    public record ComplaintStatus(boolean complained) {}

    public record ComplaintRequest(
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 500) String content
    ) {}
}

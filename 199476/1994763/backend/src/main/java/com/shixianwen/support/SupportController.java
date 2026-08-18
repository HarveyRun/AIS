package com.shixianwen.support;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {
    private final SupportService service;
    @PostMapping("/feedback") public ApiResponse<SupportService.FeedbackView> feedback(@CurrentUser User u, @Valid @RequestBody FeedbackRequest r) { return ApiResponse.ok(service.feedback(u.getId(), r.type(), r.category(), r.content(), r.targetUserId())); }
    @GetMapping("/feedback") public ApiResponse<List<SupportService.FeedbackView>> feedback(@CurrentUser User u) { return ApiResponse.ok(service.feedbackList(u.getId())); }
    @PostMapping("/business-cooperations") public ApiResponse<Long> cooperation(@CurrentUser User u, @Valid @RequestBody CooperationRequest r) { return ApiResponse.ok(service.cooperation(u.getId(), r.contact(), r.content())); }
    @PostMapping("/customer-service/messages") public ApiResponse<SupportService.CustomerServiceView> message(@CurrentUser User u, @Valid @RequestBody MessageRequest r) { return ApiResponse.ok(service.customerService(u.getId(), r.content())); }
    @PostMapping(value = "/customer-service/images", consumes = "multipart/form-data")
    public ApiResponse<SupportService.CustomerServiceView> image(
        @CurrentUser User u,
        @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.ok(service.customerServiceImage(u.getId(), image));
    }
    @GetMapping("/customer-service/messages") public ApiResponse<List<SupportService.CustomerServiceView>> messages(@CurrentUser User u) { return ApiResponse.ok(service.customerServiceList(u.getId())); }
    @GetMapping("/customer-service/unread-count") public ApiResponse<Long> unreadCount(@CurrentUser User u) { return ApiResponse.ok(service.customerServiceUnreadCount(u.getId())); }
    @PutMapping("/customer-service/read") public ApiResponse<Void> read(@CurrentUser User u) { service.readCustomerService(u.getId()); return ApiResponse.ok(); }
    public record FeedbackRequest(@NotBlank String type, @NotBlank String category, @NotBlank @Size(max=500) String content, Long targetUserId) {}
    public record CooperationRequest(@NotBlank @Size(max=50) String contact, @NotBlank @Size(max=500) String content) {}
    public record MessageRequest(@NotBlank @Size(max=500) String content) {}
}

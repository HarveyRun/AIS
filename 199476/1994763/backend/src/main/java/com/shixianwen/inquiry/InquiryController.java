package com.shixianwen.inquiry;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import com.shixianwen.user.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import com.shixianwen.network.ClientNetworkService;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {
    private final InquiryService service;
    private final ClientNetworkService clientNetworkService;
    @PostMapping
    public ApiResponse<InquiryService.InquiryView> create(
        @CurrentUser User user,
        @Valid @RequestBody CreateRequest body,
        @RequestHeader(value = "X-Client-Platform", required = false) String clientPlatform,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(service.create(
            user.getId(),
            new InquiryService.CreateCommand(
                body.answererId(), body.sourceExperienceCertificationId(), body.question(), clientPlatform
            ),
            clientNetworkService.resolve(request)
        ));
    }
    @GetMapping public ApiResponse<List<InquiryService.InquiryView>> list(@CurrentUser User u) { return ApiResponse.ok(service.list(u.getId())); }
    @GetMapping("/unread-count") public ApiResponse<Long> unreadCount(@CurrentUser User u) { return ApiResponse.ok(service.unreadCount(u.getId())); }
    @GetMapping("/{id}") public ApiResponse<InquiryService.InquiryDetail> detail(@CurrentUser User u, @PathVariable Long id) { return ApiResponse.ok(service.detail(u.getId(), id)); }
    @PutMapping("/{id}/read") public ApiResponse<Void> read(@CurrentUser User u, @PathVariable Long id) { service.read(u.getId(), id); return ApiResponse.ok(); }
    @PostMapping("/{id}/accept") public ApiResponse<InquiryService.InquiryView> accept(@CurrentUser User u, @PathVariable Long id) { return ApiResponse.ok(service.accept(u.getId(), id)); }
    @PostMapping("/{id}/reject") public ApiResponse<InquiryService.InquiryView> reject(@CurrentUser User u, @PathVariable Long id) { return ApiResponse.ok(service.reject(u.getId(), id)); }
    @PostMapping("/{id}/cancel") public ApiResponse<InquiryService.InquiryView> cancel(@CurrentUser User u, @PathVariable Long id) { return ApiResponse.ok(service.cancel(u.getId(), id)); }
    @PostMapping("/{id}/messages") public ApiResponse<InquiryService.MessageView> send(@CurrentUser User u, @PathVariable Long id, @Valid @RequestBody MessageRequest r) { return ApiResponse.ok(service.send(u.getId(), id, r.content())); }
    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    public ApiResponse<InquiryService.MessageView> sendImage(
        @CurrentUser User user,
        @PathVariable Long id,
        @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.ok(service.sendImage(user.getId(), id, image));
    }
    @PostMapping("/{id}/end")
    public ApiResponse<InquiryService.InquiryView> end(
        @CurrentUser User user,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(service.endInquiry(user.getId(), id));
    }
    public record CreateRequest(
        @NotNull Long answererId,
        @NotNull Long sourceExperienceCertificationId,
        @NotBlank @Size(max=200) String question
    ) {}
    public record MessageRequest(@NotBlank String content) {}
}

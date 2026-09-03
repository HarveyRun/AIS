package com.shixianwen.user;

import com.shixianwen.auth.AuthService;
import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.inquiry.ViolationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserService userService;
    private final ViolationService violationService;

    public UserController(UserService userService, ViolationService violationService) {
        this.userService = userService;
        this.violationService = violationService;
    }

    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    public ApiResponse<AuthService.UserView> avatar(@CurrentUser User user, @RequestPart MultipartFile avatar) {
        return ApiResponse.ok(userService.updateAvatar(user, avatar));
    }

    @GetMapping
    public ApiResponse<AuthService.UserView> me(@CurrentUser User user) {
        return ApiResponse.ok(AuthService.UserView.from(user));
    }

    @PutMapping
    public ApiResponse<AuthService.UserView> update(
        @CurrentUser User user,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.ok(userService.updateProfile(user, request.nickname(), request.avatarUrl(), request.jobTitle()));
    }

    @PatchMapping("/accepting-inquiries")
    public ApiResponse<AuthService.UserView> updateAccepting(
        @CurrentUser User user,
        @RequestBody AcceptingRequest request
    ) {
        return ApiResponse.ok(userService.setAcceptingInquiries(user, request.accepting()));
    }

    @PatchMapping("/inquiry-price-range")
    public ApiResponse<AuthService.UserView> updateInquiryPriceRange(
        @CurrentUser User user,
        @Valid @RequestBody InquiryPriceRangeRequest request
    ) {
        return ApiResponse.ok(
            userService.setInquiryPriceRange(user, request.minimum(), request.maximum())
        );
    }

    @GetMapping("/answerer-eligibility")
    public ApiResponse<AnswererEligibilityService.Eligibility> answererEligibility(
        @CurrentUser User user
    ) {
        return ApiResponse.ok(userService.answererEligibility(user));
    }

    @GetMapping("/violation-counters")
    public ApiResponse<java.util.List<ViolationService.ViolationCounterView>> violationCounters(
        @CurrentUser User user
    ) {
        return ApiResponse.ok(violationService.currentCounters(user.getId()));
    }

    @GetMapping("/deletion-eligibility")
    public ApiResponse<UserService.AccountDeletionEligibility> deletionEligibility(
        @CurrentUser User user
    ) {
        return ApiResponse.ok(userService.accountDeletionEligibility(user));
    }

    @PostMapping("/platform-introduction/dismiss")
    public ApiResponse<AuthService.UserView> dismissPlatformIntroduction(
        @CurrentUser User user
    ) {
        return ApiResponse.ok(userService.dismissPlatformIntroduction(user));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@CurrentUser User user) {
        userService.deleteAccount(user);
        return ApiResponse.ok();
    }

    public record UpdateProfileRequest(
        @Size(max = 12, message = "昵称最多12个字") String nickname,
        @Size(max = 500, message = "头像地址过长") String avatarUrl,
        @Size(max = 30, message = "岗位最多30个字") String jobTitle
    ) {
    }

    public record AcceptingRequest(boolean accepting) {
    }

    public record InquiryPriceRangeRequest(
        @NotNull @Min(1) @Max(5000) Integer minimum,
        @NotNull @Min(1) @Max(5000) Integer maximum
    ) {
    }
}

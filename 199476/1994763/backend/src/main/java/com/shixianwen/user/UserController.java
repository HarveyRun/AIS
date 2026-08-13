package com.shixianwen.user;

import com.shixianwen.auth.AuthService;
import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
        return ApiResponse.ok(userService.updateProfile(user, request.nickname(), request.avatarUrl()));
    }

    @PatchMapping("/accepting-inquiries")
    public ApiResponse<AuthService.UserView> updateAccepting(
        @CurrentUser User user,
        @RequestBody AcceptingRequest request
    ) {
        return ApiResponse.ok(userService.setAcceptingInquiries(user, request.accepting()));
    }

    @GetMapping("/answerer-eligibility")
    public ApiResponse<AnswererEligibilityService.Eligibility> answererEligibility(
        @CurrentUser User user
    ) {
        return ApiResponse.ok(userService.answererEligibility(user));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@CurrentUser User user) {
        userService.deleteAccount(user);
        return ApiResponse.ok();
    }

    public record UpdateProfileRequest(
        @Size(max = 12, message = "昵称最多12个字") String nickname,
        @Size(max = 500, message = "头像地址过长") String avatarUrl
    ) {
    }

    public record AcceptingRequest(boolean accepting) {
    }
}

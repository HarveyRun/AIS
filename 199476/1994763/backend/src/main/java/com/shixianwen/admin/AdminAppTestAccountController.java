package com.shixianwen.admin;

import com.shixianwen.auth.AppTestLoginAccountService;
import com.shixianwen.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/app-test-accounts")
@RequiredArgsConstructor
public class AdminAppTestAccountController {
    private final AppTestLoginAccountService service;

    @GetMapping
    public ApiResponse<AppTestLoginAccountService.PageResult> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.list(page, size));
    }

    @PostMapping
    public ApiResponse<AppTestLoginAccountService.View> create(
        @CurrentAdmin AdminUser admin,
        @Valid @RequestBody SaveRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.create(
            admin,
            request.phone(),
            request.verificationCode(),
            request.enabled(),
            clientIp(servletRequest)
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<AppTestLoginAccountService.View> update(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        @Valid @RequestBody SaveRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(service.update(
            admin,
            id,
            request.phone(),
            request.verificationCode(),
            request.enabled(),
            clientIp(servletRequest)
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @CurrentAdmin AdminUser admin,
        @PathVariable Long id,
        HttpServletRequest servletRequest
    ) {
        service.delete(admin, id, clientIp(servletRequest));
        return ApiResponse.ok();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null
            ? request.getRemoteAddr()
            : forwarded.split(",")[0].trim();
    }

    public record SaveRequest(
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的11位手机号")
        String phone,
        @NotBlank(message = "请输入验证码")
        @Pattern(regexp = "^\\d{4}$", message = "验证码必须是4位数字")
        String verificationCode,
        boolean enabled
    ) {
    }
}

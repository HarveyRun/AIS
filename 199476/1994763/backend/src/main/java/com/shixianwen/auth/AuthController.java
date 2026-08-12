package com.shixianwen.auth;

import com.shixianwen.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
import com.shixianwen.network.ClientNetworkService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final ClientNetworkService clientNetworkService;

    public AuthController(AuthService authService, ClientNetworkService clientNetworkService) {
        this.authService = authService;
        this.clientNetworkService = clientNetworkService;
    }

    @PostMapping("/verification-codes")
    public ApiResponse<Map<String, Object>> sendCode(@Valid @RequestBody PhoneRequest request) {
        return ApiResponse.ok(Map.of("sent", true, "expiresIn", 300));
    }

    @PostMapping("/register")
    public ApiResponse<AuthService.LoginResult> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(authService.register(
            request.phone(),
            request.nickname(),
            request.code(),
            clientNetworkService.resolve(servletRequest)
        ));
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResult> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok(authService.login(
            request.phone(),
            request.code(),
            clientNetworkService.resolve(servletRequest)
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.replaceFirst("^Bearer\\s+", ""));
        return ApiResponse.ok();
    }

    public record PhoneRequest(
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的手机号")
        String phone
    ) {
    }

    public record LoginRequest(
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的手机号") String phone,
        @NotBlank(message = "请输入验证码") String code
    ) {
    }

    public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的手机号") String phone,
        @NotBlank(message = "请输入验证码") String code,
        @Size(max = 12, message = "昵称最多12个字") String nickname
    ) {
    }
}

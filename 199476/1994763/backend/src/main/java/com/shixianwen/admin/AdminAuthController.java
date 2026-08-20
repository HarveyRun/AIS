package com.shixianwen.admin;

import com.shixianwen.common.ApiResponse;
import com.shixianwen.network.ClientNetworkService;
import com.shixianwen.realtime.RealtimeTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService service;
    private final RealtimeTicketService realtimeTickets;
    private final ClientNetworkService clientNetworkService;

    @GetMapping("/setup-status")
    public ApiResponse<Map<String, Boolean>> status() {
        return ApiResponse.ok(Map.of("needsSetup", service.needsSetup()));
    }

    @PostMapping("/setup")
    public ApiResponse<AdminAuthService.LoginResult> setup(
        @Valid @RequestBody SetupRequest body,
        @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
        HttpServletRequest request
    ) {
        String ip = clientNetworkService.resolve(request).ipAddress();
        return ApiResponse.ok(service.setup(body.phone(), body.password(), body.displayName(), ip, deviceId));
    }

    @PostMapping("/login")
    public ApiResponse<AdminAuthService.LoginResult> login(
        @Valid @RequestBody LoginRequest body,
        @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
        HttpServletRequest request
    ) {
        String ip = clientNetworkService.resolve(request).ipAddress();
        return ApiResponse.ok(service.login(body.phone(), body.password(), ip, deviceId));
    }

    @GetMapping("/me")
    public ApiResponse<AdminAuthService.AdminView> me(@CurrentAdmin AdminUser user) {
        return ApiResponse.ok(service.view(user));
    }

    @PostMapping("/realtime-ticket")
    public ApiResponse<RealtimeTicketService.TicketView> realtimeTicket(@CurrentAdmin AdminUser user) {
        return ApiResponse.ok(realtimeTickets.issueAdmin(user.getId()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        service.logout(authorization.replaceFirst("^Bearer\\s+", ""));
        return ApiResponse.ok();
    }

    public record SetupRequest(
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的手机号") String phone,
        @NotBlank @Size(min = 10, message = "密码至少10位") String password,
        @Size(max = 60, message = "显示名称最多60个字") String displayName
    ) {
    }

    public record LoginRequest(
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的手机号") String phone,
        @NotBlank @Size(min = 10, message = "密码至少10位") String password
    ) {
    }
}

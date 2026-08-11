package com.shixianwen.admin;
import com.shixianwen.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/admin/auth") @RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService service;
    @GetMapping("/setup-status") public ApiResponse<Map<String,Boolean>> status(){ return ApiResponse.ok(Map.of("needsSetup",service.needsSetup())); }
    @PostMapping("/setup") public ApiResponse<AdminAuthService.LoginResult> setup(@Valid @RequestBody Credentials r){ return ApiResponse.ok(service.setup(r.phone(),r.password(),r.displayName())); }
    @PostMapping("/login") public ApiResponse<AdminAuthService.LoginResult> login(@Valid @RequestBody Credentials r){ return ApiResponse.ok(service.login(r.phone(),r.password())); }
    @GetMapping("/me") public ApiResponse<AdminAuthService.AdminView> me(@CurrentAdmin AdminUser u){ return ApiResponse.ok(AdminAuthService.AdminView.of(u)); }
    @PostMapping("/logout") public ApiResponse<Void> logout(@RequestHeader("Authorization") String h){ service.logout(h.replaceFirst("^Bearer\\s+","")); return ApiResponse.ok(); }
    public record Credentials(@NotBlank @Pattern(regexp="^1\\d{10}$",message="请输入正确的手机号") String phone,@NotBlank @Size(min=10) String password,@Size(max=60) String displayName){}
}

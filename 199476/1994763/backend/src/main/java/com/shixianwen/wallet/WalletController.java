package com.shixianwen.wallet;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.common.BusinessException;
import com.shixianwen.network.ClientNetworkService;
import com.shixianwen.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService service;
    private final AlipayAccountAuthorizationService alipayAuthorization;
    private final ClientNetworkService clientNetworkService;

    @GetMapping
    public ApiResponse<WalletService.WalletView> wallet(@CurrentUser User user) {
        return ApiResponse.ok(service.get(user.getId()));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<WalletService.TransactionView>> transactions(@CurrentUser User user) {
        return ApiResponse.ok(service.transactions(user.getId()));
    }

    @GetMapping("/alipay-account")
    public ApiResponse<WalletService.AlipayAccountView> alipayAccount(@CurrentUser User user) {
        return ApiResponse.ok(service.alipayAccount(user.getId()));
    }

    @PostMapping("/verification-codes")
    public ApiResponse<Map<String, Object>> sendVerificationCode(
        @CurrentUser User user,
        @Valid @RequestBody VerificationCodeRequest body,
        @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
        HttpServletRequest request
    ) {
        String purpose = body.purpose().toUpperCase();
        if (!"WITHDRAWAL".equals(purpose)) {
            throw BusinessException.badRequest("验证码用途无效");
        }
        service.sendStepUpCode(
            user.getId(), purpose, clientNetworkService.resolve(request).ipAddress(), deviceId
        );
        return ApiResponse.ok(Map.of("sent", true, "expiresIn", 300));
    }

    @GetMapping("/alipay-authorization/payload")
    public ApiResponse<AlipayAccountAuthorizationService.AuthorizationPayload> alipayAuthorizationPayload(
        @CurrentUser User user
    ) {
        return ApiResponse.ok(alipayAuthorization.createPayload(user.getId()));
    }

    @PostMapping("/alipay-authorization/complete")
    public ApiResponse<WalletService.AlipayAccountView> completeAlipayAuthorization(
        @CurrentUser User user,
        @Valid @RequestBody AlipayAuthorizationCompleteRequest body,
        @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(alipayAuthorization.complete(
            user.getId(), body.authCode(),
            clientNetworkService.resolve(request).ipAddress(), deviceId
        ));
    }

    @PostMapping("/withdrawals")
    public ApiResponse<WalletService.WithdrawalView> withdraw(
        @CurrentUser User user,
        @Valid @RequestBody WithdrawalRequest body,
        @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
        HttpServletRequest request
    ) {
        return ApiResponse.ok(service.withdraw(
            user.getId(), body.amount(), body.requestId(), body.verificationCode(),
            clientNetworkService.resolve(request).ipAddress(), deviceId
        ));
    }

    @GetMapping("/withdrawals")
    public ApiResponse<List<WalletService.WithdrawalView>> withdrawals(@CurrentUser User user) {
        return ApiResponse.ok(service.withdrawals(user.getId()));
    }

    public record VerificationCodeRequest(
        @NotBlank @Pattern(regexp = "WITHDRAWAL") String purpose
    ) {
    }

    public record AlipayAuthorizationCompleteRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{8,256}") String authCode
    ) {
    }

    public record WithdrawalRequest(
        @NotNull @DecimalMin("1") @DecimalMax("9999") @Digits(integer = 4, fraction = 0) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{12,64}") String requestId,
        @NotBlank @Pattern(regexp = "^\\d{4}$") String verificationCode
    ) {
    }
}

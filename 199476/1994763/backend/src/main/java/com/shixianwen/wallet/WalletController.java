package com.shixianwen.wallet;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import com.shixianwen.user.User;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService service;

    @GetMapping public ApiResponse<WalletService.WalletView> wallet(@CurrentUser User u) { return ApiResponse.ok(service.get(u.getId())); }
    @GetMapping("/transactions") public ApiResponse<List<WalletService.TransactionView>> transactions(@CurrentUser User u) { return ApiResponse.ok(service.transactions(u.getId())); }
    @GetMapping("/bank-card") public ApiResponse<WalletService.BankCardView> bankCard(@CurrentUser User u) { return ApiResponse.ok(service.bankCard(u.getId())); }
    @PutMapping("/bank-card") public ApiResponse<WalletService.BankCardView> bankCard(@CurrentUser User u, @Valid @RequestBody BankCardRequest r) { return ApiResponse.ok(service.bindBankCard(u.getId(), r.holderName(), r.bankName(), r.cardNumber())); }
    @PostMapping("/withdrawals") public ApiResponse<WalletService.WithdrawalView> withdraw(@CurrentUser User u, @Valid @RequestBody AmountRequest r) { return ApiResponse.ok(service.withdraw(u.getId(), r.amount())); }
    @GetMapping("/withdrawals") public ApiResponse<List<WalletService.WithdrawalView>> withdrawals(@CurrentUser User u) { return ApiResponse.ok(service.withdrawals(u.getId())); }

    public record BankCardRequest(@NotBlank String holderName, @NotBlank String bankName, @NotBlank String cardNumber) {}
    public record AmountRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount) {}
}

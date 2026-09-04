package com.shixianwen.wallet;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/experience-tips")
public class ExperienceTipController {
    private final WalletService walletService;

    public ExperienceTipController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ApiResponse<WalletService.ExperienceTipView> create(
        @CurrentUser User user,
        @Valid @RequestBody Request body
    ) {
        return ApiResponse.ok(walletService.tipExperience(
            user.getId(),
            body.certificationId(),
            body.amount(),
            body.requestId()
        ));
    }

    public record Request(
        @NotNull Long certificationId,
        @NotNull @DecimalMin("1") @DecimalMax("5000") @Digits(integer = 4, fraction = 0) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{12,64}") String requestId
    ) {
    }
}

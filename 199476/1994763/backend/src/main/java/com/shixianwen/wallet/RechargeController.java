package com.shixianwen.wallet;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/recharges")
@RequiredArgsConstructor
public class RechargeController {
    private final RechargeService service;
    @PostMapping public ApiResponse<RechargeService.RechargeView> create(@CurrentUser User user, @Valid @RequestBody Request request) { return ApiResponse.ok(service.create(user.getId(), request.amount())); }
    @GetMapping public ApiResponse<List<RechargeService.RechargeView>> list(@CurrentUser User user) { return ApiResponse.ok(service.list(user.getId())); }
    @PostMapping("/payment-callback") public ApiResponse<Void> callback(@RequestHeader("X-Payment-Signature") String signature, @RequestParam String orderNo) { service.paidCallback(signature, orderNo); return ApiResponse.ok(); }
    public record Request(@NotNull @DecimalMin("0.01") BigDecimal amount) {}
}

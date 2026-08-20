package com.shixianwen.wallet;

import com.shixianwen.auth.CurrentUser;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recharges")
@RequiredArgsConstructor
public class RechargeController {
    private final RechargeService service;
    @Value("${app.payment.mock-return-url:http://localhost:5173/profile/wallet}") private String mockReturnUrl;
    @GetMapping("/capability")
    public ApiResponse<PaymentGateway.PaymentCapability> capability(@CurrentUser User user) {
        return ApiResponse.ok(service.capability(user.getId()));
    }
    @PostMapping public ApiResponse<RechargeService.RechargeView> create(@CurrentUser User user, @Valid @RequestBody Request request) { return ApiResponse.ok(service.create(user.getId(), request.amount(), request.requestId())); }
    @GetMapping public ApiResponse<List<RechargeService.RechargeView>> list(@CurrentUser User user) { return ApiResponse.ok(service.list(user.getId())); }
    @GetMapping("/{orderNo}") public ApiResponse<RechargeService.RechargeView> find(@CurrentUser User user, @PathVariable String orderNo) { return ApiResponse.ok(service.find(user.getId(), orderNo)); }
    @PostMapping("/payment-callback")
    public String callback(@RequestBody(required = false) String payload, @RequestHeader Map<String, String> headers) {
        service.paidCallback(payload == null ? "" : payload, new LinkedHashMap<>(headers));
        return "success";
    }
    @GetMapping(value = "/mock-cashier", produces = MediaType.TEXT_HTML_VALUE)
    public String mockCashier(@RequestParam String orderNo) {
        RechargeService.RechargeView order = service.mockOrder(orderNo);
        return "<!doctype html><html lang=\"zh-CN\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
            "<title>支付宝模拟收银台</title><style>body{margin:0;background:#f5f5f5;font-family:sans-serif;color:#222}" +
            ".card{max-width:420px;margin:60px auto;background:#fff;padding:28px;border-radius:18px;box-shadow:0 8px 30px #0001}" +
            "h1{font-size:20px}strong{display:block;font-size:34px;margin:28px 0;color:#1677ff}button{width:100%;border:0;border-radius:12px;padding:14px;background:#1677ff;color:#fff;font-size:16px}</style>" +
            "<div class=\"card\"><h1>支付宝模拟收银台</h1><p>订单号：" + order.orderNo() + "</p><strong>¥" + order.amount().stripTrailingZeros().toPlainString() + "</strong>" +
            "<form method=\"post\" action=\"/api/recharges/mock-payment\"><input type=\"hidden\" name=\"orderNo\" value=\"" + order.orderNo() + "\">" +
            "<button type=\"submit\">确认支付</button></form></div></html>";
    }
    @PostMapping(value = "/mock-payment", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> mockPayment(@RequestParam String orderNo) {
        service.completeMockPayment(orderNo);
        return ResponseEntity.status(303)
            .location(URI.create(mockReturnUrl + "?recharge=success&orderNo=" + orderNo))
            .build();
    }
    public record Request(
        @NotNull @DecimalMin("1") @DecimalMax("9999") @Digits(integer=4, fraction=0) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{12,64}") String requestId
    ) {}
}

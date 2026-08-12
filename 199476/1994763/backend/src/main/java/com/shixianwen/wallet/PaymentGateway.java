package com.shixianwen.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public interface PaymentGateway {
    PaymentCapability capability();

    PaymentOrder createOrder(String orderNo, BigDecimal amount, String subject);

    PaymentStatus queryOrder(String orderNo);

    PaymentNotification verifyNotification(String payload, Map<String, String> headers);

    record PaymentCapability(String channel, String name, boolean available, String message) {}

    record PaymentOrder(
        String orderNo,
        String channel,
        String providerTradeNo,
        String paymentUrl,
        String status
    ) {}

    record PaymentStatus(
        String orderNo,
        String providerTradeNo,
        String status,
        BigDecimal paidAmount,
        LocalDateTime paidAt
    ) {}

    record PaymentNotification(
        String orderNo,
        String providerTradeNo,
        String status,
        BigDecimal paidAmount,
        LocalDateTime paidAt
    ) {}
}

package com.shixianwen.wallet;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentOrder createOrder(String orderNo, BigDecimal amount, String subject);
    record PaymentOrder(String orderNo, String channel, String paymentUrl, String status) {}
}

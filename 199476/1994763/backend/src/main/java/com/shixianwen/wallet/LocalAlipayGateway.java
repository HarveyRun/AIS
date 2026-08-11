package com.shixianwen.wallet;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("local")
public class LocalAlipayGateway implements PaymentGateway {
    @Override
    public PaymentOrder createOrder(String orderNo, BigDecimal amount, String subject) {
        return new PaymentOrder(orderNo, "ALIPAY", null, "WAITING_FOR_PAYMENT");
    }
}

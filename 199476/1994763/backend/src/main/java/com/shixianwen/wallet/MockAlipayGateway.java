package com.shixianwen.wallet;

import com.shixianwen.common.BusinessException;

import java.math.BigDecimal;
import java.util.Map;

/** 开发环境支付宝模拟器，完整测试充值流程，不连接第三方。 */
public class MockAlipayGateway implements PaymentGateway {
    @Override
    public PaymentCapability capability() {
        return new PaymentCapability("ALIPAY", "支付宝", true, "开发环境模拟支付");
    }

    @Override
    public PaymentOrder createOrder(String orderNo, BigDecimal amount, String subject) {
        String paymentUrl = "/api/recharges/mock-cashier?orderNo=" + orderNo;
        return new PaymentOrder(orderNo, "ALIPAY", null, paymentUrl, "WAITING_FOR_PAYMENT");
    }

    @Override
    public PaymentStatus queryOrder(String orderNo) {
        return new PaymentStatus(orderNo, null, "WAITING_FOR_PAYMENT", null, null);
    }

    @Override
    public PaymentNotification verifyNotification(String payload, Map<String, String> headers) {
        throw BusinessException.forbidden("模拟支付不接收外部回调");
    }
}

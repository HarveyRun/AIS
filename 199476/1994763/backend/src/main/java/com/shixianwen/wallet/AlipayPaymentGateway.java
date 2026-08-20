package com.shixianwen.wallet;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.shixianwen.common.BusinessException;
import com.shixianwen.integration.ThirdPartySettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "alipay")
public class AlipayPaymentGateway implements PaymentGateway {
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String appId;
    private final String privateKey;
    private final String publicKey;
    private final String notifyUrl;
    private final AlipayClient client;

    public AlipayPaymentGateway(ThirdPartySettings settings) {
        String gatewayUrl = settings.value("app.payment.alipay.gateway-url", "alipay.server-url");
        this.appId = settings.value("app.payment.alipay.app-id", "alipay.app-id");
        this.privateKey = settings.value("app.payment.alipay.private-key");
        this.publicKey = settings.value("app.payment.alipay.public-key");
        this.notifyUrl = normalizeLegacyNotifyUrl(
            settings.value("app.payment.alipay.notify-url", "alipay.notify-url")
        );
        this.client = new DefaultAlipayClient(
            gatewayUrl, appId, privateKey, "json", CHARSET, publicKey, SIGN_TYPE
        );
    }

    @Override
    public PaymentCapability capability() {
        boolean configured = hasText(appId) && hasText(privateKey) && hasText(publicKey);
        return new PaymentCapability(
            "ALIPAY",
            "支付宝",
            configured,
            configured ? "可用" : "支付宝参数未配置完整",
            "ALIPAY_APP"
        );
    }

    @Override
    public PaymentOrder createOrder(String orderNo, BigDecimal amount, String subject) {
        ensureConfigured();
        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setOutTradeNo(orderNo);
        model.setTotalAmount(amount.toPlainString());
        model.setSubject(subject);
        model.setProductCode("QUICK_MSECURITY_PAY");

        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        request.setBizModel(model);
        if (hasText(notifyUrl)) {
            request.setNotifyUrl(notifyUrl);
        }
        try {
            AlipayTradeAppPayResponse response = client.sdkExecute(request);
            if (!response.isSuccess() || !hasText(response.getBody())) {
                throw BusinessException.serviceUnavailable("支付宝支付单创建失败");
            }
            return new PaymentOrder(
                orderNo,
                "ALIPAY",
                null,
                response.getBody(),
                "WAITING_FOR_PAYMENT"
            );
        } catch (AlipayApiException exception) {
            throw BusinessException.serviceUnavailable("支付宝支付单创建失败");
        }
    }

    @Override
    public PaymentStatus queryOrder(String orderNo) {
        ensureConfigured();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(orderNo);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizModel(model);
        try {
            AlipayTradeQueryResponse response = client.execute(request);
            if (!response.isSuccess()) {
                return new PaymentStatus(orderNo, null, "WAITING_FOR_PAYMENT", null, null);
            }
            String status = switch (response.getTradeStatus()) {
                case "TRADE_SUCCESS", "TRADE_FINISHED" -> "PAID";
                case "TRADE_CLOSED" -> "CLOSED";
                default -> "WAITING_FOR_PAYMENT";
            };
            String returnedOrderNo = response.getOutTradeNo();
            if (hasText(returnedOrderNo) && !orderNo.equals(returnedOrderNo)) {
                throw BusinessException.badRequest("支付宝返回的订单号与平台订单不一致");
            }
            return new PaymentStatus(
                hasText(returnedOrderNo) ? returnedOrderNo : orderNo,
                response.getTradeNo(),
                status,
                decimal(response.getTotalAmount()),
                null
            );
        } catch (AlipayApiException exception) {
            throw BusinessException.serviceUnavailable("支付宝订单查询失败");
        }
    }

    @Override
    public PaymentNotification verifyNotification(String payload, Map<String, String> headers) {
        ensureConfigured();
        Map<String, String> parameters = parseForm(payload);
        try {
            boolean valid = AlipaySignature.rsaCheckV1(parameters, publicKey, CHARSET, SIGN_TYPE);
            if (!valid || !appId.equals(parameters.get("app_id"))) {
                throw BusinessException.forbidden("支付宝回调验签失败");
            }
        } catch (AlipayApiException exception) {
            throw BusinessException.forbidden("支付宝回调验签失败");
        }

        String tradeStatus = parameters.get("trade_status");
        String status = ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))
            ? "PAID"
            : "IGNORED";
        if ("PAID".equals(status)) {
            requireCallbackValue(parameters, "out_trade_no", "平台订单号");
            requireCallbackValue(parameters, "trade_no", "支付宝交易号");
            requireCallbackValue(parameters, "total_amount", "实付金额");
        }
        return new PaymentNotification(
            parameters.get("out_trade_no"),
            parameters.get("trade_no"),
            status,
            decimal(parameters.get("total_amount")),
            dateTime(parameters.get("gmt_payment"))
        );
    }

    private void ensureConfigured() {
        if (!capability().available()) {
            throw BusinessException.serviceUnavailable(capability().message());
        }
    }

    private static Map<String, String> parseForm(String payload) {
        Map<String, String> values = new LinkedHashMap<>();
        if (payload == null || payload.isBlank()) return values;
        for (String pair : payload.split("&")) {
            String[] parts = pair.split("=", 2);
            String name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(name, value);
        }
        return values;
    }

    private static String normalizeLegacyNotifyUrl(String value) {
        if (value == null) return "";
        return value.replace("/pay/ali/callback", "/api/recharges/payment-callback");
    }

    private static BigDecimal decimal(String value) {
        if (!hasText(value)) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw BusinessException.badRequest("支付宝返回的金额格式不正确");
        }
    }

    private static LocalDateTime dateTime(String value) {
        return hasText(value) ? LocalDateTime.parse(value, ALIPAY_TIME) : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void requireCallbackValue(
        Map<String, String> parameters,
        String name,
        String label
    ) {
        if (!hasText(parameters.get(name))) {
            throw BusinessException.badRequest("支付宝回调缺少" + label);
        }
    }
}

package com.shixianwen.wallet;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.shixianwen.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
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
    private final String publicKey;
    private final String notifyUrl;
    private final String returnUrl;
    private final AlipayClient client;

    public AlipayPaymentGateway(
        @Value("${app.payment.alipay.gateway-url}") String gatewayUrl,
        @Value("${app.payment.alipay.app-id}") String appId,
        @Value("${app.payment.alipay.private-key}") String privateKey,
        @Value("${app.payment.alipay.public-key}") String publicKey,
        @Value("${app.payment.alipay.notify-url}") String notifyUrl,
        @Value("${app.payment.alipay.return-url}") String returnUrl
    ) {
        this.appId = appId;
        this.publicKey = publicKey;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
        this.client = new DefaultAlipayClient(
            gatewayUrl, appId, privateKey, "json", CHARSET, publicKey, SIGN_TYPE
        );
    }

    @Override
    public PaymentCapability capability() {
        boolean configured = hasText(appId) && hasText(publicKey) && hasText(notifyUrl);
        return new PaymentCapability(
            "ALIPAY",
            "支付宝",
            configured,
            configured ? "可用" : "支付宝参数未配置完整"
        );
    }

    @Override
    public PaymentOrder createOrder(String orderNo, BigDecimal amount, String subject) {
        ensureConfigured();
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(orderNo);
        model.setTotalAmount(amount.toPlainString());
        model.setSubject(subject);
        model.setProductCode("QUICK_WAP_WAY");

        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setBizModel(model);
        request.setNotifyUrl(notifyUrl);
        if (hasText(returnUrl)) request.setReturnUrl(returnUrl);
        try {
            String paymentUrl = client.pageExecute(request, "GET").getBody();
            return new PaymentOrder(orderNo, "ALIPAY", null, paymentUrl, "WAITING_FOR_PAYMENT");
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
            return new PaymentStatus(
                orderNo,
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

    private static BigDecimal decimal(String value) {
        return hasText(value) ? new BigDecimal(value) : null;
    }

    private static LocalDateTime dateTime(String value) {
        return hasText(value) ? LocalDateTime.parse(value, ALIPAY_TIME) : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

package com.shixianwen.wallet;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import com.shixianwen.common.BusinessException;
import com.shixianwen.integration.ThirdPartySettings;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AlipayAccountAuthorizationService {
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";

    private final String gatewayUrl;
    private final String appId;
    private final String partnerId;
    private final String privateKey;
    private final String publicKey;
    private final AlipayClient client;
    private final UserRepository users;
    private final AlipayAccountRepository accounts;
    private final AccountCipher accountCipher;
    private final SecurityEventService securityEvents;

    public AlipayAccountAuthorizationService(
        ThirdPartySettings settings,
        UserRepository users,
        AlipayAccountRepository accounts,
        AccountCipher accountCipher,
        SecurityEventService securityEvents
    ) {
        this.gatewayUrl = settings.value("app.payment.alipay.gateway-url", "alipay.server-url");
        this.appId = settings.value("app.payment.alipay.app-id", "alipay.app-id");
        this.partnerId = settings.value("app.payment.alipay.partner-id", "alipay.partner-id");
        this.privateKey = settings.value("app.payment.alipay.private-key");
        this.publicKey = settings.value("app.payment.alipay.public-key");
        this.client = new DefaultAlipayClient(
            gatewayUrl, appId, privateKey, "json", CHARSET, publicKey, SIGN_TYPE
        );
        this.users = users;
        this.accounts = accounts;
        this.accountCipher = accountCipher;
        this.securityEvents = securityEvents;
    }

    public AuthorizationPayload createPayload(Long userId) {
        User user = user(userId);
        if ("TEST".equals(user.getAccountType())) {
            throw BusinessException.forbidden("测试账号不能绑定真实支付宝账户");
        }
        ensureConfigured(true);

        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("apiname", "com.alipay.account.auth");
        parameters.put("app_id", appId);
        parameters.put("app_name", "mc");
        parameters.put("auth_type", "AUTHACCOUNT");
        parameters.put("biz_type", "openservice");
        parameters.put("method", "alipay.open.auth.sdk.code.get");
        parameters.put("pid", partnerId);
        parameters.put("product_id", "APP_FAST_LOGIN");
        parameters.put("scope", "kuaijie");
        parameters.put("sign_type", SIGN_TYPE);
        parameters.put("target_id", UUID.randomUUID().toString().replace("-", ""));

        String content = join(parameters, false);
        try {
            String signature = AlipaySignature.rsaSign(content, privateKey, CHARSET, SIGN_TYPE);
            return new AuthorizationPayload(joinWithSignature(parameters, signature));
        } catch (AlipayApiException exception) {
            throw BusinessException.serviceUnavailable("支付宝授权信息生成失败");
        }
    }

    @Transactional
    public WalletService.AlipayAccountView complete(
        Long userId,
        String authCode,
        String ipAddress,
        String deviceId
    ) {
        User user = user(userId);
        if ("TEST".equals(user.getAccountType())) {
            throw BusinessException.forbidden("测试账号不能绑定真实支付宝账户");
        }
        ensureConfigured(false);
        String code = authCode == null ? "" : authCode.trim();
        if (!code.matches("[A-Za-z0-9_-]{8,256}")) {
            throw BusinessException.badRequest("支付宝授权结果无效，请重新授权");
        }

        AlipaySystemOauthTokenResponse token = exchangeCode(code);
        Identifier identifier = identifier(token);
        AlipayUserInfoShareResponse information = userInformation(token.getAccessToken());
        String fingerprint = accountCipher.fingerprint(identifier.type() + ":" + identifier.value());
        accounts.findByAlipayUserIdHash(fingerprint)
            .filter(existing -> !existing.getUser().getId().equals(userId))
            .ifPresent(existing -> {
                throw BusinessException.badRequest("该支付宝账户已授权给其他平台账号");
            });

        AlipayAccount account = accounts.findByUserId(userId).orElseGet(AlipayAccount::new);
        account.setUser(user);
        account.setAuthorizationType("OAUTH");
        account.setIdentifierType(identifier.type());
        account.setAccountCiphertext(accountCipher.encrypt(identifier.value()));
        account.setAlipayUserIdHash(fingerprint);
        account.setAccountMasked(mask(identifier.value()));
        account.setRealName(trim(firstText(
            information == null ? null : information.getUserName(),
            information == null ? null : information.getDisplayName()
        ), 80));
        account.setDisplayName(trim(firstText(
            information == null ? null : information.getDisplayName(),
            information == null ? null : information.getNickName(),
            information == null ? null : information.getUserName(),
            "已授权支付宝账户"
        ), 120));
        account.setAuthorizedAt(LocalDateTime.now());
        account = accounts.save(account);

        securityEvents.recordSafely(
            userId, null, "ALIPAY_ACCOUNT_AUTHORIZED", "HIGH", ipAddress, deviceId,
            "identifierType=" + identifier.type() + ", account=" + account.getAccountMasked()
        );
        return WalletService.AlipayAccountView.of(account);
    }

    private AlipaySystemOauthTokenResponse exchangeCode(String authCode) {
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setGrantType("authorization_code");
        request.setCode(authCode);
        try {
            AlipaySystemOauthTokenResponse response = client.execute(request);
            if (!response.isSuccess() || !hasText(response.getAccessToken())) {
                throw BusinessException.badRequest("支付宝授权未完成，请重新授权");
            }
            return response;
        } catch (AlipayApiException exception) {
            throw BusinessException.serviceUnavailable("支付宝授权结果确认失败");
        }
    }

    private AlipayUserInfoShareResponse userInformation(String accessToken) {
        try {
            AlipayUserInfoShareResponse response = client.execute(new AlipayUserInfoShareRequest(), accessToken);
            return response.isSuccess() ? response : null;
        } catch (AlipayApiException exception) {
            return null;
        }
    }

    private Identifier identifier(AlipaySystemOauthTokenResponse response) {
        if (hasText(response.getUserId())) return new Identifier("USER_ID", response.getUserId().trim());
        if (hasText(response.getAlipayUserId())) {
            return new Identifier("ALIPAY_USER_ID", response.getAlipayUserId().trim());
        }
        if (hasText(response.getOpenId())) return new Identifier("OPEN_ID", response.getOpenId().trim());
        throw BusinessException.badRequest("支付宝未返回可用于收款的账户标识");
    }

    private User user(Long userId) {
        return users.findById(userId).orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    private void ensureConfigured(boolean requirePartnerId) {
        boolean configured = hasText(gatewayUrl) && hasText(appId) && hasText(privateKey) && hasText(publicKey);
        if (requirePartnerId) configured = configured && hasText(partnerId);
        if (!configured) {
            throw BusinessException.serviceUnavailable(
                requirePartnerId ? "支付宝授权参数未配置完整，请联系平台" : "支付宝参数未配置完整，请联系平台"
            );
        }
    }

    private String joinWithSignature(Map<String, String> parameters, String signature) {
        return join(parameters, true) + "&sign=" + encode(signature);
    }

    private String join(Map<String, String> parameters, boolean encodeValues) {
        return parameters.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + (encodeValues ? encode(entry.getValue()) : entry.getValue()))
            .collect(java.util.stream.Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String mask(String value) {
        if (value.length() <= 8) return value.substring(0, 2) + "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) return value.trim();
        }
        return null;
    }

    private String trim(String value, int maximumLength) {
        if (value == null) return null;
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record AuthorizationPayload(String authPayload) {
    }

    private record Identifier(String type, String value) {
    }
}

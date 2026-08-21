package com.shixianwen.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 短信供应商接入点。接入真实短信服务时替换此实现即可，测试账号的固定验证码逻辑位于 AuthService。
 */
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "mock")
public class DeferredSmsVerificationCodeSender implements VerificationCodeSender {
    @Override
    public void send(String phone, String code) {
        // 本地模式不调用第三方，验证码由 VerificationCodeService 使用受控固定值生成。
    }

    @Override
    public boolean localMode() {
        return true;
    }
}

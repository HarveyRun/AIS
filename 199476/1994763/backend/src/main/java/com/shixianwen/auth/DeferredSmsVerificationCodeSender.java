package com.shixianwen.auth;

import org.springframework.stereotype.Component;

/**
 * 短信供应商接入点。接入真实短信服务时替换此实现即可，测试账号的绕过逻辑位于 AuthService。
 */
@Component
public class DeferredSmsVerificationCodeSender implements VerificationCodeSender {
    @Override
    public void send(String phone) {
        // 当前环境保留完整调用边界，不伪造第三方短信发送结果。
    }
}

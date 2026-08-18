package com.shixianwen.auth;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.shixianwen.common.BusinessException;
import com.shixianwen.integration.ThirdPartySettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "aliyun", matchIfMissing = true)
public class AliyunSmsVerificationCodeSender implements VerificationCodeSender {
    private final ThirdPartySettings settings;

    public AliyunSmsVerificationCodeSender(ThirdPartySettings settings) {
        this.settings = settings;
    }

    @Override
    public void send(String phone, String code) {
        String accessKeyId = settings.value("app.sms.aliyun.access-key-id");
        String accessKeySecret = settings.value("app.sms.aliyun.access-key-secret");
        String endpoint = settings.value("app.sms.aliyun.endpoint");
        String signName = settings.value("app.sms.aliyun.sign-name");
        String templateCode = settings.value("app.sms.aliyun.template-code");
        if (!hasText(accessKeyId) || !hasText(accessKeySecret) || !hasText(signName) || !hasText(templateCode)) {
            throw BusinessException.serviceUnavailable("短信服务配置不完整");
        }
        try {
            Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
            config.endpoint = hasText(endpoint) ? endpoint : "dysmsapi.aliyuncs.com";
            Client client = new Client(config);
            SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setTemplateParam("{\"name\":\"" + code + "\"}");
            SendSmsResponse response = client.sendSms(request);
            String resultCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equals(resultCode)) {
                throw BusinessException.serviceUnavailable("验证码发送失败，请稍后重试");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw BusinessException.serviceUnavailable("验证码发送失败，请稍后重试");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

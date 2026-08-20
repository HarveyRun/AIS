package com.shixianwen.storage;

import com.shixianwen.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class LocalMediaSigner {
    private final byte[] secret;

    public LocalMediaSigner(
        @Value("${app.storage.local-signing-secret:${app.auth.verification-code-pepper}}") String secret
    ) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalStateException("本地私有文件签名密钥至少需要16位");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String signedUrl(String storageKey) {
        long expires = Instant.now().plusSeconds(600).getEpochSecond();
        return "/api/public/local-media?key=" + storageKey
            + "&expires=" + expires + "&signature=" + signature(storageKey, expires);
    }

    public void verify(String storageKey, long expires, String signature) {
        if (storageKey == null || !storageKey.startsWith("private/")
            || expires < Instant.now().getEpochSecond() || expires > Instant.now().plusSeconds(900).getEpochSecond()) {
            throw BusinessException.forbidden("文件地址已失效");
        }
        String expected = signature(storageKey, expires);
        if (signature == null || !MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.US_ASCII),
            signature.getBytes(StandardCharsets.US_ASCII)
        )) {
            throw BusinessException.forbidden("文件地址已失效");
        }
    }

    private String signature(String storageKey, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(
                (storageKey + ':' + expires).getBytes(StandardCharsets.UTF_8)
            ));
        } catch (Exception exception) {
            throw new IllegalStateException("私有文件签名失败", exception);
        }
    }
}

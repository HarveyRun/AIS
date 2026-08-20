package com.shixianwen.wallet;

import com.shixianwen.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AccountCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AccountCipher(
        @Value("${app.wallet.account-encryption-secret:${app.auth.verification-code-pepper}}") String secret
    ) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalStateException("收款账户加密密钥至少需要16个字符");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("无法初始化收款账户加密", exception);
        }
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array()
            );
        } catch (Exception exception) {
            throw BusinessException.badRequest("收款账户保存失败，请稍后重试");
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] value = Base64.getDecoder().decode(ciphertext);
            if (value.length <= IV_LENGTH) throw new IllegalArgumentException("invalid ciphertext");
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[value.length - IV_LENGTH];
            System.arraycopy(value, 0, iv, 0, IV_LENGTH);
            System.arraycopy(value, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw BusinessException.badRequest("收款账户信息无法读取，请重新授权");
        }
    }

    public String fingerprint(String plaintext) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getEncoded(), "HmacSHA256"));
            byte[] digest = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw BusinessException.badRequest("收款账户校验失败，请稍后重试");
        }
    }
}

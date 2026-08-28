package com.shixianwen.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PhoneIdentityHash {
    private PhoneIdentityHash() {
    }

    public static String of(String phone) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                digest.digest(phone.trim().getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持SHA-256", exception);
        }
    }
}

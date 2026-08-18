package com.shixianwen.auth;

public interface VerificationCodeSender {
    void send(String phone, String code);

    default boolean localMode() {
        return false;
    }
}

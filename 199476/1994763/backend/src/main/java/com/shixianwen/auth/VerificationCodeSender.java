package com.shixianwen.auth;

public interface VerificationCodeSender {
    void send(String phone);
}

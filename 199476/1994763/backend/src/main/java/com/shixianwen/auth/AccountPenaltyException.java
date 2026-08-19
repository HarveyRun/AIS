package com.shixianwen.auth;

import com.shixianwen.common.BusinessException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class AccountPenaltyException extends BusinessException {
    private final String reason;
    private final LocalDateTime banUntil;

    public AccountPenaltyException(String reason, LocalDateTime banUntil) {
        super(HttpStatus.FORBIDDEN, message(reason, banUntil));
        this.reason = reason;
        this.banUntil = banUntil;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getBanUntil() {
        return banUntil;
    }

    public boolean isPermanent() {
        return banUntil == null;
    }

    private static String message(String reason, LocalDateTime banUntil) {
        String duration = banUntil == null ? "账号已被永久封禁" : "账号已被限期封禁";
        return duration + "，处罚原因：" + reason;
    }
}

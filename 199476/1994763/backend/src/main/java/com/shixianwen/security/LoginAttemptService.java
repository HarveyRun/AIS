package com.shixianwen.security;

import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private final LoginAttemptRepository repository;
    private final SecurityEventService securityEvents;

    @Transactional(readOnly = true)
    public void requireAllowed(String type, String phone, String ip, String deviceId) {
        LocalDateTime after = LocalDateTime.now().minusMinutes(15);
        long phoneFailures = repository.countBySubjectTypeAndPhoneAndSuccessfulFalseAndCreatedAtAfter(type, phone, after);
        long ipFailures = repository.countBySubjectTypeAndRequestIpAndSuccessfulFalseAndCreatedAtAfter(type, safeIp(ip), after);
        long deviceFailures = repository.countBySubjectTypeAndDeviceIdAndSuccessfulFalseAndCreatedAtAfter(type, safeDevice(deviceId), after);
        int phoneLimit = "ADMIN".equals(type) ? 5 : 10;
        int ipLimit = "ADMIN".equals(type) ? 10 : 30;
        int deviceLimit = "ADMIN".equals(type) ? 8 : 20;
        if (phoneFailures >= phoneLimit || ipFailures >= ipLimit || deviceFailures >= deviceLimit) {
            securityEvents.recordSafely(null, null, type + "_LOGIN_BLOCKED", "HIGH", ip, deviceId,
                "15分钟内失败次数过多");
            throw BusinessException.tooManyRequests("尝试次数过多，请15分钟后再试");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String type, String phone, String ip, String deviceId, boolean successful) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setSubjectType(type);
        attempt.setPhone(phone);
        attempt.setRequestIp(safeIp(ip));
        attempt.setDeviceId(safeDevice(deviceId));
        attempt.setSuccessful(successful);
        repository.save(attempt);
    }

    public static String safeDevice(String value) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.matches("[A-Za-z0-9._:-]{8,100}")) return "unknown";
        return normalized;
    }

    private static String safeIp(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? "unknown" : normalized.substring(0, Math.min(normalized.length(), 45));
    }
}

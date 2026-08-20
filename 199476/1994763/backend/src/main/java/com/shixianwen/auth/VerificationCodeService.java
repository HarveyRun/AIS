package com.shixianwen.auth;

import com.shixianwen.common.BusinessException;
import com.shixianwen.security.LoginAttemptService;
import com.shixianwen.security.SecurityEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class VerificationCodeService {
    private static final int MAX_ATTEMPTS = 5;
    private static final int PHONE_DAILY_LIMIT = 10;
    private static final int IP_DAILY_LIMIT = 40;
    private static final int DEVICE_DAILY_LIMIT = 20;
    private static final Object[] SEND_LOCKS = new Object[256];

    static {
        for (int index = 0; index < SEND_LOCKS.length; index++) {
            SEND_LOCKS[index] = new Object();
        }
    }

    private final VerificationCodeRepository repository;
    private final VerificationCodeSender sender;
    private final SecurityEventService securityEvents;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.auth.verification-code-pepper}")
    private String pepper;

    @Value("${app.auth.demo-verification-code:1234}")
    private String localCode;

    @Transactional
    public void send(String phone, String purpose, String requestIp, String deviceId) {
        String safePurpose = normalizePurpose(purpose);
        String safeDevice = LoginAttemptService.safeDevice(deviceId);
        String safeIp = requestIp == null || requestIp.isBlank() ? "unknown" : requestIp;
        Object sendLock = SEND_LOCKS[Math.floorMod((phone + ':' + safePurpose).hashCode(), SEND_LOCKS.length)];
        synchronized (sendLock) {
            sendLocked(phone, safePurpose, safeIp, safeDevice);
        }
    }

    private void sendLocked(String phone, String safePurpose, String safeIp, String safeDevice) {
        LocalDateTime now = LocalDateTime.now();

        repository.findFirstByPhoneAndPurposeOrderByCreatedAtDesc(phone, safePurpose).ifPresent(latest -> {
            if (latest.getCreatedAt() != null && latest.getCreatedAt().isAfter(now.minusSeconds(60))) {
                recordSendBlocked(phone, safePurpose, safeIp, safeDevice, "COOLDOWN");
                throw BusinessException.tooManyRequests("验证码发送过于频繁，请稍后再试");
            }
        });

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        if (repository.countByPhoneAndPurposeAndCreatedAtAfter(phone, safePurpose, startOfDay) >= PHONE_DAILY_LIMIT) {
            recordSendBlocked(phone, safePurpose, safeIp, safeDevice, "PHONE_DAILY");
            throw BusinessException.tooManyRequests("该手机号今日获取验证码次数已达上限");
        }
        if (repository.countByRequestIpAndCreatedAtAfter(safeIp, startOfDay) >= IP_DAILY_LIMIT) {
            recordSendBlocked(phone, safePurpose, safeIp, safeDevice, "IP_DAILY");
            throw BusinessException.tooManyRequests("当前网络今日获取验证码次数已达上限");
        }
        if (repository.countByRequestDeviceIdAndCreatedAtAfter(safeDevice, startOfDay) >= DEVICE_DAILY_LIMIT) {
            recordSendBlocked(phone, safePurpose, safeIp, safeDevice, "DEVICE_DAILY");
            throw BusinessException.tooManyRequests("当前设备今日获取验证码次数已达上限");
        }

        String code = sender.localMode()
            ? localCode
            : String.valueOf(1000 + random.nextInt(9000));
        sender.send(phone, code);

        VerificationCode record = new VerificationCode();
        record.setPhone(phone);
        record.setPurpose(safePurpose);
        record.setCodeHash(hash(phone, safePurpose, code));
        record.setRequestIp(safeIp);
        record.setRequestDeviceId(safeDevice);
        record.setExpiresAt(now.plusMinutes(5));
        repository.save(record);
    }

    private void recordSendBlocked(
        String phone,
        String purpose,
        String ip,
        String deviceId,
        String reason
    ) {
        String suffix = phone == null || phone.length() < 4
            ? "unknown"
            : phone.substring(phone.length() - 4);
        securityEvents.recordSafely(
            null, null, "SMS_SEND_BLOCKED", "COOLDOWN".equals(reason) ? "MEDIUM" : "HIGH", ip, deviceId,
            "reason=" + reason + ", purpose=" + purpose + ", phoneSuffix=" + suffix
        );
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void verify(String phone, String purpose, String code) {
        String safePurpose = normalizePurpose(purpose);
        LocalDateTime now = LocalDateTime.now();
        VerificationCode record = repository
            .findFirstByPhoneAndPurposeAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                phone,
                safePurpose,
                now
            )
            .orElseThrow(() -> BusinessException.badRequest("验证码已失效，请重新获取"));
        if (record.getAttempts() >= MAX_ATTEMPTS) {
            record.setConsumedAt(now);
            throw BusinessException.badRequest("验证码已失效，请重新获取");
        }
        if (!MessageDigest.isEqual(
            record.getCodeHash().getBytes(StandardCharsets.UTF_8),
            hash(phone, safePurpose, code).getBytes(StandardCharsets.UTF_8)
        )) {
            record.setAttempts(record.getAttempts() + 1);
            if (record.getAttempts() >= MAX_ATTEMPTS) {
                record.setConsumedAt(now);
            }
            throw BusinessException.badRequest("验证码不正确");
        }
        record.setConsumedAt(now);
    }

    private String hash(String phone, String purpose, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String value = phone + ':' + purpose + ':' + code + ':' + pepper;
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String normalizePurpose(String purpose) {
        String value = purpose == null ? "LOGIN" : purpose.trim().toUpperCase();
        return switch (value) {
            case "LOGIN", "WITHDRAWAL" -> value;
            default -> throw BusinessException.badRequest("验证码用途无效");
        };
    }
}

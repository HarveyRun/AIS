package com.shixianwen.auth;

import com.shixianwen.common.BusinessException;
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
    private static final int PHONE_DAILY_LIMIT = 20;
    private static final int IP_DAILY_LIMIT = 60;

    private final VerificationCodeRepository repository;
    private final VerificationCodeSender sender;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.auth.verification-code-pepper}")
    private String pepper;

    @Value("${app.auth.demo-verification-code:1234}")
    private String localCode;

    @Transactional
    public void send(String phone, String requestIp) {
        LocalDateTime now = LocalDateTime.now();
        repository.findFirstByPhoneOrderByCreatedAtDesc(phone).ifPresent(latest -> {
            if (latest.getCreatedAt() != null && latest.getCreatedAt().isAfter(now.minusSeconds(60))) {
                throw BusinessException.badRequest("验证码发送过于频繁，请稍后再试");
            }
        });
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        if (repository.countByPhoneAndCreatedAtAfter(phone, startOfDay) >= PHONE_DAILY_LIMIT) {
            throw BusinessException.badRequest("该手机号今日获取验证码次数已达上限");
        }
        String safeIp = requestIp == null || requestIp.isBlank() ? "unknown" : requestIp;
        if (repository.countByRequestIpAndCreatedAtAfter(safeIp, startOfDay) >= IP_DAILY_LIMIT) {
            throw BusinessException.badRequest("当前网络今日获取验证码次数已达上限");
        }

        String code = sender.localMode()
            ? localCode
            : String.valueOf(1000 + random.nextInt(9000));
        sender.send(phone, code);

        VerificationCode record = new VerificationCode();
        record.setPhone(phone);
        record.setCodeHash(hash(phone, code));
        record.setRequestIp(safeIp);
        record.setExpiresAt(now.plusMinutes(5));
        repository.save(record);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void verify(String phone, String code) {
        LocalDateTime now = LocalDateTime.now();
        VerificationCode record = repository
            .findFirstByPhoneAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(phone, now)
            .orElseThrow(() -> BusinessException.badRequest("验证码已失效，请重新获取"));
        if (record.getAttempts() >= MAX_ATTEMPTS) {
            record.setConsumedAt(now);
            throw BusinessException.badRequest("验证码已失效，请重新获取");
        }
        if (!MessageDigest.isEqual(
            record.getCodeHash().getBytes(StandardCharsets.UTF_8),
            hash(phone, code).getBytes(StandardCharsets.UTF_8)
        )) {
            record.setAttempts(record.getAttempts() + 1);
            if (record.getAttempts() >= MAX_ATTEMPTS) {
                record.setConsumedAt(now);
            }
            throw BusinessException.badRequest("验证码不正确");
        }
        record.setConsumedAt(now);
    }

    private String hash(String phone, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String value = phone + ':' + code + ':' + pepper;
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

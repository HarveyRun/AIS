package com.shixianwen.invitation;

import com.shixianwen.notification.NotificationService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.PermanentBanPayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvitationSubmissionGuard {
    private static final int PERMANENT_BAN_ATTEMPTS = 9;

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final NotificationService notifications;
    private final RealtimePublisher realtime;
    private final PermanentBanPayoutService permanentBanPayouts;
    private final SecurityEventService securityEvents;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordInvalid(
        Long userId,
        String ipAddress,
        String deviceId,
        String invitedUid
    ) {
        User user = users.findWithLockById(userId).orElseThrow();
        jdbc.update(
            "INSERT INTO invitation_submission_guards(user_id,invalid_attempts,last_invalid_at) " +
                "VALUES(?,1,NOW(6)) ON DUPLICATE KEY UPDATE " +
                "invalid_attempts=invalid_attempts+1,last_invalid_at=NOW(6)",
            userId
        );
        Integer attempts = jdbc.queryForObject(
            "SELECT invalid_attempts FROM invitation_submission_guards WHERE user_id=? FOR UPDATE",
            Integer.class,
            userId
        );
        int count = attempts == null ? 1 : attempts;
        securityEvents.recordSafely(
            userId,
            null,
            "INVITATION_REWARD_INVALID_IDENTITY",
            count >= PERMANENT_BAN_ATTEMPTS ? "CRITICAL" : "HIGH",
            ipAddress,
            deviceId,
            "invitedUid=" + safe(invitedUid) + ",attempts=" + count
        );
        if (count >= PERMANENT_BAN_ATTEMPTS) {
            permanentlyBan(user);
        }
        return count;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clear(Long userId) {
        jdbc.update(
            "UPDATE invitation_submission_guards SET invalid_attempts=0,cleared_at=NOW(6) " +
                "WHERE user_id=? AND invalid_attempts>0",
            userId
        );
    }

    private void permanentlyBan(User user) {
        if ("SUSPENDED".equals(user.getAccountStatus()) && user.getBanUntil() == null) {
            return;
        }
        user.setAccountStatus("SUSPENDED");
        user.setBanReason("违反平台规则");
        user.setBannedAt(LocalDateTime.now());
        user.setBanUntil(null);
        user.setBannedByAdmin(null);
        users.saveAndFlush(user);
        permanentBanPayouts.captureForPermanentBan(user.getId());
        notifications.send(
            user,
            "账号处罚通知",
            "账号因违反平台规则已被永久封禁",
            "/profile"
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", "违反平台规则");
        payload.put("permanent", true);
        realtime.afterCommit(user.getId(), "ACCOUNT_PENALTY", payload);
    }

    private String safe(String value) {
        if (value == null) return "";
        String clean = value.replaceAll("[^0-9]", "");
        return clean.substring(0, Math.min(clean.length(), 20));
    }
}

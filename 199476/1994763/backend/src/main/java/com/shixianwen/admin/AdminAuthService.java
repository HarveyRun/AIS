package com.shixianwen.admin;

import com.shixianwen.common.BusinessException;
import com.shixianwen.security.LoginAttemptService;
import com.shixianwen.security.SecurityEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final AdminUserRepository users;
    private final AdminSessionRepository sessions;
    private final AdminPasswordEncoder passwords;
    private final JdbcTemplate jdbc;
    private final LoginAttemptService loginAttempts;
    private final SecurityEventService securityEvents;
    private final AdminAuthorizationService authorization;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public boolean needsSetup() {
        return users.count() == 0;
    }

    @Transactional
    public LoginResult setup(
        String phone,
        String password,
        String displayName,
        String ip,
        String deviceId
    ) {
        boolean hasAdmin = users.count() > 0;
        jdbc.update(
            "INSERT IGNORE INTO admin_system_state(id,initialized,initialized_at) VALUES (1,?,?)",
            hasAdmin,
            hasAdmin ? LocalDateTime.now() : null
        );
        if (hasAdmin) {
            jdbc.update(
                "UPDATE admin_system_state SET initialized=TRUE,initialized_at=COALESCE(initialized_at,NOW(6)) WHERE id=1"
            );
            throw BusinessException.forbidden("管理员已经初始化");
        }
        jdbc.update(
            "UPDATE admin_system_state SET initialized=FALSE,initialized_at=NULL WHERE id=1 AND NOT EXISTS (SELECT 1 FROM admin_users)"
        );
        int claimed = jdbc.update(
            "UPDATE admin_system_state SET initialized=TRUE,initialized_at=NOW(6) WHERE id=1 AND initialized=FALSE"
        );
        if (claimed != 1) throw BusinessException.forbidden("管理员已经初始化");
        validate(phone, password);
        AdminUser user = new AdminUser();
        user.setPhone(phone.trim());
        user.setPasswordHash(passwords.encode(password));
        user.setMustChangePassword(false);
        user.setDisplayName(displayName == null || displayName.isBlank() ? "管理员" : displayName.trim());
        user = users.save(user);
        securityEvents.recordSafely(null, null, "ADMIN_CREATED", "HIGH", ip, deviceId, "phoneSuffix=" + phone.substring(7));
        assignSuperAdminRole(user.getId());
        return session(user, ip, deviceId);
    }

    @Transactional
    public LoginResult login(
        String phone,
        String password,
        String ip,
        String deviceId
    ) {
        String safeDevice = LoginAttemptService.safeDevice(deviceId);
        loginAttempts.requireAllowed("ADMIN", phone, ip, safeDevice);
        AdminUser user = users.findByPhoneAndStatusAndDeletedAtIsNull(phone, "ACTIVE").orElse(null);
        if (!passwords.matches(password, user == null ? null : user.getPasswordHash())) {
            loginAttempts.record("ADMIN", phone, ip, safeDevice, false);
            securityEvents.recordSafely(null, null, "ADMIN_LOGIN_FAILED", "HIGH", ip, safeDevice, "登录校验失败");
            throw unauthorized();
        }
        if (authorization.roles(user.getId()).isEmpty()) {
            securityEvents.recordSafely(
                null, user.getId(), "ADMIN_LOGIN_NO_ACTIVE_ROLE", "HIGH", ip, safeDevice,
                "账号没有可用角色"
            );
            throw BusinessException.forbidden("账号没有可用角色，请联系超级管理员");
        }
        user.setLastLoginAt(LocalDateTime.now());
        LoginResult result = session(user, ip, safeDevice);
        loginAttempts.record("ADMIN", phone, ip, safeDevice, true);
        securityEvents.recordSafely(null, user.getId(), "ADMIN_LOGIN_SUCCESS", "MEDIUM", ip, safeDevice, null);
        return result;
    }

    @Transactional(readOnly = true)
    public AdminUser authenticate(String token, String ip, String deviceId) {
        AdminSession session = sessions.findByTokenHashAndExpiresAtAfter(hash(token), LocalDateTime.now())
            .orElseThrow(() -> unauthorizedSession());
        String safeDevice = LoginAttemptService.safeDevice(deviceId);
        if (!session.getLoginIp().equals(ip) || !session.getDeviceId().equals(safeDevice)) {
            securityEvents.recordSafely(
                null, session.getAdminUser().getId(), "ADMIN_SESSION_CONTEXT_CHANGED", "CRITICAL",
                ip, safeDevice, "后台会话的网络或设备发生变化"
            );
            throw unauthorizedSession();
        }
        AdminUser user = session.getAdminUser();
        if (!"ACTIVE".equals(user.getStatus())) throw unauthorizedSession();
        return user;
    }

    @Transactional
    public void logout(String token) {
        sessions.deleteByTokenHash(hash(token));
    }

    @Transactional
    public void changePassword(
        AdminUser user,
        String currentPassword,
        String newPassword,
        String ip,
        String deviceId
    ) {
        validatePassword(newPassword);
        if (!passwords.matches(currentPassword, user.getPasswordHash())) {
            securityEvents.recordSafely(
                null, user.getId(), "ADMIN_PASSWORD_CHANGE_FAILED", "HIGH", ip,
                LoginAttemptService.safeDevice(deviceId), "当前密码校验失败"
            );
            throw BusinessException.badRequest("当前密码不正确");
        }
        if (passwords.matches(newPassword, user.getPasswordHash())) {
            throw BusinessException.badRequest("新密码不能与当前密码相同");
        }
        if (AdminAccountManagementService.DEFAULT_RESET_PASSWORD.equals(newPassword)) {
            throw BusinessException.badRequest("新密码不能继续使用平台默认密码");
        }
        user.setPasswordHash(passwords.encode(newPassword));
        user.setMustChangePassword(false);
        users.save(user);
        securityEvents.recordSafely(
            null, user.getId(), "ADMIN_PASSWORD_CHANGED", "HIGH", ip,
            LoginAttemptService.safeDevice(deviceId), null
        );
    }

    private LoginResult session(AdminUser user, String ip, String deviceId) {
        sessions.deleteByExpiresAtBefore(LocalDateTime.now());
        sessions.deleteByAdminUserId(user.getId());
        sessions.flush();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = HexFormat.of().formatHex(bytes);
        AdminSession session = new AdminSession();
        session.setAdminUser(user);
        session.setTokenHash(hash(raw));
        session.setLoginIp(ip == null || ip.isBlank() ? "unknown" : ip);
        session.setDeviceId(LoginAttemptService.safeDevice(deviceId));
        session.setExpiresAt(LocalDateTime.now().plusHours(2));
        sessions.save(session);
        return new LoginResult(raw, view(user));
    }

    @Transactional(readOnly = true)
    public AdminView view(AdminUser user) {
        return AdminView.of(
            user,
            authorization.roles(user.getId()),
            authorization.permissionCodes(user.getId())
        );
    }

    private void assignSuperAdminRole(Long userId) {
        jdbc.update(
            "INSERT IGNORE INTO admin_user_roles(admin_user_id,role_id) " +
                "SELECT ?,id FROM admin_roles WHERE code='SUPER_ADMIN' AND deleted_at IS NULL",
            userId
        );
    }

    private void validate(String phone, String password) {
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw BusinessException.badRequest("请输入正确的手机号");
        }
        validatePassword(password);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 10 || password.length() > 128
            || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw BusinessException.badRequest("密码需为10至128位，并包含字母和数字");
        }
    }

    private BusinessException unauthorized() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码不正确");
    }

    private BusinessException unauthorizedSession() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "管理端登录已失效");
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record AdminView(
        Long id,
        String phone,
        String displayName,
        String role,
        boolean mustChangePassword,
        java.util.List<AdminAuthorizationService.AdminRoleSummary> roles,
        java.util.Set<String> permissions
    ) {
        static AdminView of(
            AdminUser user,
            java.util.List<AdminAuthorizationService.AdminRoleSummary> roles,
            java.util.Set<String> permissions
        ) {
            return new AdminView(
                user.getId(), user.getPhone(), user.getDisplayName(), user.getRole(),
                user.isMustChangePassword(), roles, permissions
            );
        }
    }

    public record LoginResult(String token, AdminView admin) {
    }
}

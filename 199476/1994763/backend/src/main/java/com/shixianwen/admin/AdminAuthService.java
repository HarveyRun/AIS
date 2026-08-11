package com.shixianwen.admin;

import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;

@Service @RequiredArgsConstructor
public class AdminAuthService {
    private final AdminUserRepository users;
    private final AdminSessionRepository sessions;
    private final AdminPasswordEncoder passwords;
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public boolean needsSetup() {
        Boolean initialized = jdbc.queryForObject("SELECT initialized FROM admin_system_state WHERE id=1", Boolean.class);
        return !Boolean.TRUE.equals(initialized);
    }
    @Transactional
    public LoginResult setup(String phone, String password, String displayName) {
        int claimed = jdbc.update("UPDATE admin_system_state SET initialized=TRUE,initialized_at=NOW(6) WHERE id=1 AND initialized=FALSE");
        if (claimed != 1) throw BusinessException.forbidden("管理员已经初始化");
        validate(phone, password);
        AdminUser user = new AdminUser(); user.setPhone(phone.trim()); user.setPasswordHash(passwords.encode(password));
        user.setDisplayName(displayName == null || displayName.isBlank() ? "管理员" : displayName.trim()); users.save(user);
        return session(user);
    }
    @Transactional
    public LoginResult login(String phone, String password) {
        AdminUser user = users.findByPhoneAndStatus(phone, "ACTIVE")
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码不正确"));
        if (!passwords.matches(password, user.getPasswordHash())) throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码不正确");
        user.setLastLoginAt(LocalDateTime.now()); return session(user);
    }
    @Transactional(readOnly = true)
    public AdminUser authenticate(String token) {
        return sessions.findByTokenHashAndExpiresAtAfter(hash(token), LocalDateTime.now()).map(AdminSession::getAdminUser)
                .filter(u -> "ACTIVE".equals(u.getStatus())).orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "管理端登录已失效"));
    }
    @Transactional public void logout(String token) { sessions.deleteByTokenHash(hash(token)); }
    private LoginResult session(AdminUser user) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes); String raw = HexFormat.of().formatHex(bytes);
        AdminSession session = new AdminSession(); session.setAdminUser(user); session.setTokenHash(hash(raw));
        session.setExpiresAt(LocalDateTime.now().plusHours(12)); sessions.save(session);
        return new LoginResult(raw, AdminView.of(user));
    }
    private void validate(String phone, String password) {
        if (phone == null || !phone.matches("^1\\d{10}$")) throw BusinessException.badRequest("请输入正确的手机号");
        if (password == null || password.length() < 10 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) throw BusinessException.badRequest("密码至少10位，并包含字母和数字");
    }
    private static String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch(Exception e) { throw new IllegalStateException(e); } }
    public record AdminView(Long id, String phone, String displayName, String role) { static AdminView of(AdminUser u) { return new AdminView(u.getId(), u.getPhone(), u.getDisplayName(), u.getRole()); } }
    public record LoginResult(String token, AdminView admin) {}
}

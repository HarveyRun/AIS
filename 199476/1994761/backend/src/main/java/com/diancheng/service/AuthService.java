package com.diancheng.service;

import com.diancheng.api.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public static final String ADMIN_EMAIL = "timeline.1994.1976@gmail.com";
    public static final String MASTER_INVITE = "DC-199476";
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public BCryptPasswordEncoder encoder() {
        return encoder;
    }

    public Optional<String> currentEmail(HttpServletRequest request) {
        String token = currentToken(request).orElse("");
        if (token.isEmpty()) return Optional.empty();
        return jdbc.query("SELECT user_email FROM auth_sessions WHERE token=? AND expires_at>NOW(3)",
                (rs, row) -> rs.getString(1), token).stream().findFirst();
    }

    public Optional<String> currentToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
        String token = header.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    public String requireUser(HttpServletRequest request) {
        return currentEmail(request).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录。"));
    }

    public String requireAdmin(HttpServletRequest request) {
        String email = requireUser(request);
        if (!ADMIN_EMAIL.equals(email)) throw new ApiException(HttpStatus.FORBIDDEN, "只有管理员可以执行该操作。");
        return email;
    }

    @Transactional
    public Map<String, Object> login(String emailInput, String password) {
        String email = normalizeEmail(emailInput);
        if (email.length() > 190 || !email.matches("^\\S+@\\S+\\.\\S+$")) return fail("请输入有效的邮箱地址。");
        if (password == null || password.length() < 8 || password.length() > 72) return fail("密码长度应为 8—72 位。");
        var rows = jdbc.queryForList("SELECT password_hash FROM users WHERE email=?", email);
        if (rows.isEmpty()) return fail("账号不存在，请先注册。");
        if (!encoder.matches(password, String.valueOf(rows.get(0).get("password_hash")))) return fail("密码不正确。");
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        jdbc.update("DELETE FROM auth_sessions WHERE user_email=? OR expires_at<=NOW(3)", email);
        jdbc.update("INSERT INTO auth_sessions(token,user_email,created_at,expires_at) VALUES(?,?,?,?)",
                token, email, Timestamp.from(now), Timestamp.from(now.plus(30, ChronoUnit.DAYS)));
        return Map.of("ok", true, "email", email, "token", token);
    }

    @Transactional
    public Map<String, Object> register(String emailInput, String password, String confirmation, String inviteDigits) {
        String email = normalizeEmail(emailInput);
        if (email.length() > 190 || !email.matches("^\\S+@\\S+\\.\\S+$")) return fail("请输入有效的邮箱地址。");
        if (password == null || password.length() < 8 || password.length() > 72) return fail("密码长度应为 8—72 位。");
        if (!password.equals(confirmation)) return fail("两次输入的密码不一致。");
        if (inviteDigits == null || !inviteDigits.trim().matches("^\\d{6}$")) return fail("请输入 6 位数字邀请码。");
        if (exists("SELECT COUNT(*) FROM users WHERE email=?", email)) return fail("该邮箱已经注册，请直接登录。");
        String usedInvite = "DC-" + inviteDigits.trim();
        if (!MASTER_INVITE.equals(usedInvite) && !exists("SELECT COUNT(*) FROM users WHERE invite_code=?", usedInvite))
            return fail("邀请码无效。");
        String inviteCode = createInvite(email);
        Instant now = Instant.now();
        jdbc.update("INSERT INTO users(email,name,password_hash,invite_code,used_invite_code,balance,is_admin,created_at) VALUES(?,?,?,?,?,0,false,?)",
                email, "", encoder.encode(password), inviteCode, usedInvite, Timestamp.from(now));
        return login(email, password);
    }

    public void logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) jdbc.update("DELETE FROM auth_sessions WHERE token=?", header.substring(7).trim());
    }

    public String normalizeEmail(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }

    private boolean exists(String sql, Object value) {
        Integer count = jdbc.queryForObject(sql, Integer.class, value);
        return count != null && count > 0;
    }

    private String createInvite(String email) {
        long value = Integer.toUnsignedLong(email.hashCode()) % 1_000_000L;
        String candidate = "DC-%06d".formatted(value);
        while (exists("SELECT COUNT(*) FROM users WHERE invite_code=?", candidate)) {
            value = (value + 7919) % 1_000_000L;
            candidate = "DC-%06d".formatted(value);
        }
        return candidate;
    }

    private Map<String, Object> fail(String message) {
        return Map.of("ok", false, "error", message);
    }
}

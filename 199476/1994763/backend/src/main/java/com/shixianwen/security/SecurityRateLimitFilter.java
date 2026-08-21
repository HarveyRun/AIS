package com.shixianwen.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.network.ClientIpExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class SecurityRateLimitFilter extends OncePerRequestFilter {
    private final ClientIpExtractor ipExtractor;
    private final ObjectMapper objectMapper;
    private final SecurityEventService securityEvents;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        Rule rule = rule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = Instant.now().toEpochMilli();
        String ip = ipExtractor.extract(request);
        String device = LoginAttemptService.safeDevice(request.getHeader("X-Device-Id"));
        String identity = identity(request, ip, device);
        Decision ipDecision = consume(rule, rule.name() + ":ip:" + ip, now);
        Decision identityDecision = identity.equals("ip:" + ip)
            ? ipDecision
            : consume(rule, rule.name() + ':' + identity, now);
        opportunisticCleanup(now);

        if (ipDecision.exceeded() || identityDecision.exceeded()) {
            if (ipDecision.firstExceeded() || identityDecision.firstExceeded()) {
                securityEvents.recordSafely(
                    null, null, "API_RATE_LIMIT", "HIGH", ip, device,
                    "rule=" + rule.name() + ", path=" + safePath(request.getRequestURI())
                );
            }
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponse.error("操作过于频繁，请稍后再试"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Decision consume(Rule rule, String key, long now) {
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now >= current.expiresAt()) {
                return new Window(1, now + rule.windowMillis(), false);
            }
            int nextCount = current.count() + 1;
            boolean logged = current.logged() || nextCount > rule.limit();
            return new Window(nextCount, current.expiresAt(), logged);
        });
        boolean exceeded = window.count() > rule.limit();
        return new Decision(exceeded, exceeded && window.count() == rule.limit() + 1);
    }

    private Rule rule(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("/api/auth/verification-codes".equals(path) && "POST".equals(method)) {
            return new Rule("SMS", 5, 60_000);
        }
        if ("/api/auth/login".equals(path) && "POST".equals(method)) {
            return new Rule("USER_LOGIN", 10, 60_000);
        }
        if ("/api/admin/auth/setup".equals(path) && "POST".equals(method)) {
            return new Rule("ADMIN_SETUP", 5, 15 * 60_000L);
        }
        if ("/api/admin/auth/login".equals(path) && "POST".equals(method)) {
            return new Rule("ADMIN_LOGIN", 20, 60_000L);
        }
        if ("/api/admin/auth/change-password".equals(path) && "POST".equals(method)) {
            return new Rule("ADMIN_PASSWORD_CHANGE", 10, 15 * 60_000L);
        }
        if ((path.startsWith("/api/admin/admin-users")
            || path.startsWith("/api/admin/roles")
            || path.startsWith("/api/admin/permissions"))
            && !"GET".equals(method) && !"HEAD".equals(method)) {
            return new Rule("ADMIN_RBAC_MUTATION", 30, 60_000L);
        }
        if ((path.contains("/images") || path.contains("/materials") || path.contains("/avatar"))
            && "POST".equals(method)) {
            return new Rule("UPLOAD", 20, 60_000);
        }
        if (path.matches("/api/inquiries/\\d+/(messages|images)") && "POST".equals(method)) {
            return new Rule("CHAT", 60, 60_000);
        }
        if ((path.startsWith("/api/wallet") || path.startsWith("/api/recharges"))
            && !"GET".equals(method)) {
            return new Rule("MONEY", 20, 60_000);
        }
        if ("GET".equals(method)
            && (path.startsWith("/api/public/discovery") || path.startsWith("/api/answerers"))) {
            return new Rule("SEARCH", 120, 60_000);
        }
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return new Rule("MUTATION", 120, 60_000);
        }
        if ("GET".equals(method) && path.startsWith("/api/")) {
            return new Rule("READ", 300, 60_000);
        }
        return null;
    }

    private String identity(HttpServletRequest request, String ip, String device) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return "token:" + hash(authorization.substring(7));
        }
        if (!"unknown".equals(device)) return "device:" + device;
        return "ip:" + ip;
    }

    private void opportunisticCleanup(long now) {
        if ((now & 63) != 0 || windows.size() < 1000) return;
        windows.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt());
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            ).substring(0, 24);
        } catch (Exception exception) {
            return "unknown";
        }
    }

    private static String safePath(String path) {
        if (path == null) return "";
        return path.length() <= 200 ? path : path.substring(0, 200);
    }

    private record Rule(String name, int limit, long windowMillis) {
    }

    private record Window(int count, long expiresAt, boolean logged) {
    }

    private record Decision(boolean exceeded, boolean firstExceeded) {
    }
}

package com.shixianwen.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.network.ClientIpExtractor;
import com.shixianwen.security.SecurityEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminPermissionInterceptor implements HandlerInterceptor {
    private final AdminPermissionResolver resolver;
    private final AdminAuthorizationService authorization;
    private final ObjectMapper mapper;
    private final ClientIpExtractor clientIpExtractor;
    private final SecurityEventService securityEvents;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) return true;
        String required = resolver.resolve(request.getMethod(), request.getRequestURI());
        if (required == null) return true;
        AdminUser admin = (AdminUser) request.getAttribute(AdminAuthInterceptor.ATTRIBUTE);
        boolean allowed = admin != null && !"__DENY__".equals(required)
            && java.util.Arrays.stream(required.split("\\|"))
                .anyMatch(code -> authorization.hasPermission(admin, code));
        if (allowed) {
            return true;
        }
        securityEvents.recordSafely(
            null,
            admin == null ? null : admin.getId(),
            "ADMIN_ACCESS_DENIED",
            "HIGH",
            clientIpExtractor.extract(request),
            request.getHeader("X-Device-Id"),
            "method=" + request.getMethod() + ", path=" + request.getRequestURI() + ", required=" + required
        );
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), ApiResponse.error("没有权限执行此操作"));
        return false;
    }
}

package com.shixianwen.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.common.ApiResponse;
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
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), ApiResponse.error("没有权限执行此操作"));
        return false;
    }
}

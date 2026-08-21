package com.shixianwen.admin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.common.ApiResponse;
import com.shixianwen.common.BusinessException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.shixianwen.network.ClientIpExtractor;
import com.shixianwen.security.LoginAttemptService;
@Component @RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {
    public static final String ATTRIBUTE = "currentAdmin";
    private final AdminAuthService auth; private final ObjectMapper mapper;
    private final ClientIpExtractor clientIpExtractor;
    @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if ("OPTIONS".equals(req.getMethod())) return true;
        String value=req.getHeader("Authorization");
        if(value==null || !value.startsWith("Bearer ")) { unauthorized(res); return false; }
        try {
            String deviceId = LoginAttemptService.safeDevice(req.getHeader("X-Device-Id"));
            AdminUser admin = auth.authenticate(value.substring(7), clientIpExtractor.extract(req), deviceId);
            if (admin.isMustChangePassword() && !allowedBeforePasswordChange(req.getRequestURI())) {
                forbiddenPasswordChange(res);
                return false;
            }
            req.setAttribute(ATTRIBUTE, admin);
            return true;
        }
        catch(BusinessException e) { unauthorized(res); return false; }
    }
    private void unauthorized(HttpServletResponse res) throws Exception { res.setStatus(401); res.setContentType(MediaType.APPLICATION_JSON_VALUE); res.setCharacterEncoding("UTF-8"); mapper.writeValue(res.getWriter(), ApiResponse.error("管理端登录已失效")); }
    private void forbiddenPasswordChange(HttpServletResponse res) throws Exception { res.setStatus(403); res.setContentType(MediaType.APPLICATION_JSON_VALUE); res.setCharacterEncoding("UTF-8"); mapper.writeValue(res.getWriter(), ApiResponse.error("请先修改初始密码")); }
    private boolean allowedBeforePasswordChange(String path) {
        return "/api/admin/auth/me".equals(path)
            || "/api/admin/auth/change-password".equals(path)
            || "/api/admin/auth/logout".equals(path);
    }
}

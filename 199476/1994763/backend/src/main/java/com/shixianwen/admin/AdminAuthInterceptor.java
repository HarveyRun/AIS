package com.shixianwen.admin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.common.ApiResponse;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component @RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {
    public static final String ATTRIBUTE = "currentAdmin";
    private final AdminAuthService auth; private final ObjectMapper mapper;
    @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if ("OPTIONS".equals(req.getMethod())) return true;
        String value=req.getHeader("Authorization");
        if(value==null || !value.startsWith("Bearer ")) { unauthorized(res); return false; }
        try { req.setAttribute(ATTRIBUTE, auth.authenticate(value.substring(7))); return true; }
        catch(RuntimeException e) { unauthorized(res); return false; }
    }
    private void unauthorized(HttpServletResponse res) throws Exception { res.setStatus(401); res.setContentType(MediaType.APPLICATION_JSON_VALUE); res.setCharacterEncoding("UTF-8"); mapper.writeValue(res.getWriter(), ApiResponse.error("管理端登录已失效")); }
}

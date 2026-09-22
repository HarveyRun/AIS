package com.shixianwen.certification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Reject before Spring's multipart resolver can spool an old client's archive to disk. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "oss")
public class LegacyExperienceMultipartRejectFilter extends OncePerRequestFilter {
    private final ObjectMapper json;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String contentType = request.getContentType();
        if ("POST".equals(request.getMethod())
            && "/api/certifications/experiences".equals(request.getRequestURI())
            && contentType != null
            && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(response.getWriter(), ApiResponse.error("请更新 App 后使用证明资料直传"));
            return;
        }
        chain.doFilter(request, response);
    }
}

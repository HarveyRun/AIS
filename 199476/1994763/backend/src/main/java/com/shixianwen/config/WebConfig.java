package com.shixianwen.config;

import com.shixianwen.auth.AuthInterceptor;
import com.shixianwen.auth.CurrentUserArgumentResolver;
import com.shixianwen.admin.AdminAuthInterceptor;
import com.shixianwen.admin.CurrentAdminResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final String[] allowedOrigins;
    private final Path storageRoot;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final CurrentAdminResolver currentAdminResolver;

    public WebConfig(
        AuthInterceptor authInterceptor,
        CurrentUserArgumentResolver currentUserArgumentResolver,
        @Value("${app.cors.allowed-origins}") String allowedOrigins,
        @Value("${app.storage.root}") String storageRoot,
        AdminAuthInterceptor adminAuthInterceptor,
        CurrentAdminResolver currentAdminResolver
    ) {
        this.authInterceptor = authInterceptor;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new);
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.currentAdminResolver = currentAdminResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/**",
                "/api/admin/**",
                "/api/public/**",
                "/api/recharges/payment-callback",
                "/actuator/**",
                "/uploads/**"
            );
        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api/admin/**")
            .excludePathPatterns("/api/admin/auth/setup-status", "/api/admin/auth/setup", "/api/admin/auth/login");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
        resolvers.add(currentAdminResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(storageRoot.toUri().toString());
    }
}

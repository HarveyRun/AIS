package com.shixianwen.admin;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.*;
import org.springframework.web.method.support.*;
@Component
public class CurrentAdminResolver implements HandlerMethodArgumentResolver {
    public boolean supportsParameter(MethodParameter p) { return p.hasParameterAnnotation(CurrentAdmin.class) && p.getParameterType().equals(AdminUser.class); }
    public Object resolveArgument(MethodParameter p, ModelAndViewContainer m, NativeWebRequest r, WebDataBinderFactory b) { return r.getAttribute(AdminAuthInterceptor.ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST); }
}

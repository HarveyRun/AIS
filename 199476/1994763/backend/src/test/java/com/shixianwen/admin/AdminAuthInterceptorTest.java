package com.shixianwen.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.common.BusinessException;
import com.shixianwen.network.ClientIpExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAuthInterceptorTest {
    @Test
    void authenticationFailureReturnsUnauthorized() throws Exception {
        AdminAuthService auth = mock(AdminAuthService.class);
        ClientIpExtractor ips = mock(ClientIpExtractor.class);
        when(ips.extract(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        when(auth.authenticate(anyString(), anyString(), anyString()))
            .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效"));
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(auth, new ObjectMapper(), ips);
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void infrastructureFailureIsNotMisreportedAsExpiredLogin() {
        AdminAuthService auth = mock(AdminAuthService.class);
        ClientIpExtractor ips = mock(ClientIpExtractor.class);
        when(ips.extract(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        when(auth.authenticate(anyString(), anyString(), anyString()))
            .thenThrow(new DataAccessResourceFailureException("database unavailable"));
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(auth, new ObjectMapper(), ips);

        assertThatThrownBy(() -> interceptor.preHandle(request(), new MockHttpServletResponse(), new Object()))
            .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void initialPasswordCanOnlyAccessPasswordChangeBoundary() throws Exception {
        AdminAuthService auth = mock(AdminAuthService.class);
        ClientIpExtractor ips = mock(ClientIpExtractor.class);
        AdminUser admin = new AdminUser();
        admin.setMustChangePassword(true);
        when(ips.extract(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        when(auth.authenticate(anyString(), anyString(), anyString())).thenReturn(admin);
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(auth, new ObjectMapper(), ips);

        MockHttpServletResponse businessResponse = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(request(), businessResponse, new Object())).isFalse();
        assertThat(businessResponse.getStatus()).isEqualTo(403);

        MockHttpServletRequest changeRequest = request();
        changeRequest.setRequestURI("/api/admin/auth/change-password");
        assertThat(interceptor.preHandle(changeRequest, new MockHttpServletResponse(), new Object())).isTrue();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard");
        request.addHeader("Authorization", "Bearer test-token");
        request.addHeader("X-Device-Id", "admin-device-001");
        return request;
    }
}

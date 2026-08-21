package com.shixianwen.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.network.ClientIpExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SecurityRateLimitFilterTest {
    private SecurityRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SecurityRateLimitFilter(
            new ClientIpExtractor("127.0.0.1,::1"),
            new ObjectMapper(),
            mock(SecurityEventService.class)
        );
    }

    @Test
    void realtimeTicketsAndLogoutDoNotConsumeAdminLoginLimit() throws Exception {
        for (int index = 0; index < 5; index++) {
            assertAllowed("/api/admin/auth/realtime-ticket");
        }
        assertAllowed("/api/admin/auth/logout");
        assertAllowed("/api/admin/auth/login");
    }

    @Test
    void adminLoginStillHasAnIndependentRequestLimit() throws Exception {
        for (int index = 0; index < 20; index++) {
            assertAllowed("/api/admin/auth/login");
        }

        MockHttpServletResponse response = execute("/api/admin/auth/login");

        assertEquals(429, response.getStatus());
    }

    private void assertAllowed(String path) throws Exception {
        assertEquals(200, execute(path).getStatus());
    }

    private MockHttpServletResponse execute(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Device-Id", "admin-device-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}

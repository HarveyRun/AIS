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

    @Test
    void authenticatedUsersOnTheSameIpHaveIndependentReadLimits() throws Exception {
        for (int index = 0; index < 200; index++) {
            assertEquals(200, executeRead("token-a").getStatus());
            assertEquals(200, executeRead("token-b").getStatus());
        }
    }

    @Test
    void oneAuthenticatedUserStillCannotExceedTheReadLimit() throws Exception {
        for (int index = 0; index < 300; index++) {
            assertEquals(200, executeRead("single-token").getStatus());
        }
        assertEquals(429, executeRead("single-token").getStatus());
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

    private MockHttpServletResponse executeRead(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/inquiries");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Device-Id", "device-" + token);
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}

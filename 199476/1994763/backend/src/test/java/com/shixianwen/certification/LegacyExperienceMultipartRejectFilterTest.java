package com.shixianwen.certification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class LegacyExperienceMultipartRejectFilterTest {
    @Test
    void blocksLegacyArchiveBeforeMultipartResolution() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "POST", "/api/certifications/experiences"
        );
        request.setContentType("multipart/form-data; boundary=legacy");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new LegacyExperienceMultipartRejectFilter(new ObjectMapper())
            .doFilter(request, response, chain);

        assertEquals(415, response.getStatus());
        verifyNoInteractions(chain);
    }
}

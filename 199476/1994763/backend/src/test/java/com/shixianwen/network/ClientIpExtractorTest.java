package com.shixianwen.network;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpExtractorTest {
    private final ClientIpExtractor extractor = new ClientIpExtractor("127.0.0.1,::1");

    @Test
    void usesForwardedAddressOnlyForTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.8, 127.0.0.1");

        assertThat(extractor.extract(request)).isEqualTo("203.0.113.8");
    }

    @Test
    void ignoresForwardedAddressFromUntrustedClient() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");
        request.addHeader("X-Forwarded-For", "203.0.113.8");

        assertThat(extractor.extract(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void ignoresInvalidForwardedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "not-an-ip");

        assertThat(extractor.extract(request)).isEqualTo("127.0.0.1");
    }
}

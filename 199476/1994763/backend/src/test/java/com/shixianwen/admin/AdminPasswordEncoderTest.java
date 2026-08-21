package com.shixianwen.admin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPasswordEncoderTest {
    private final AdminPasswordEncoder encoder = new AdminPasswordEncoder();

    @Test
    void nullStoredHashUsesDummyHashInsteadOfSkippingPasswordWork() {
        assertThat(encoder.matches("any-password", null)).isFalse();
    }

    @Test
    void encodedPasswordRemainsCaseSensitive() {
        String encoded = encoder.encode("123456abcAbc");

        assertThat(encoder.matches("123456abcAbc", encoded)).isTrue();
        assertThat(encoder.matches("123456abcABC", encoded)).isFalse();
    }
}

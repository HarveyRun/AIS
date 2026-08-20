package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.security.SecurityEventService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatAbuseGuardTest {
    @Test
    void blocksMessageFloodInsideOneMinute() {
        InquiryMessageRepository messages = mock(InquiryMessageRepository.class);
        when(messages.countByInquiryIdAndSenderIdAndCreatedAtAfter(
            eq(9L), eq(7L), any(LocalDateTime.class)
        )).thenReturn(20L);
        ChatAbuseGuard guard = new ChatAbuseGuard(messages, mock(SecurityEventService.class));

        assertThatThrownBy(() -> guard.requireTextAllowed(9L, 7L, "hello"))
            .isInstanceOf(BusinessException.class);
    }
}

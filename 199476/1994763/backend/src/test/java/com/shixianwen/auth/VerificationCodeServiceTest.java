package com.shixianwen.auth;

import com.shixianwen.common.BusinessException;
import com.shixianwen.security.SecurityEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {
    @Mock
    private VerificationCodeRepository repository;

    @Mock
    private VerificationCodeSender sender;

    private VerificationCodeService service;

    @BeforeEach
    void setUp() {
        service = new VerificationCodeService(repository, sender, org.mockito.Mockito.mock(SecurityEventService.class));
        ReflectionTestUtils.setField(service, "pepper", "test-pepper");
        ReflectionTestUtils.setField(service, "localCode", "1234");
        when(repository.findFirstByPhoneAndPurposeOrderByCreatedAtDesc("13800138000", "LOGIN"))
            .thenReturn(Optional.empty());
    }

    @Test
    void sendsAndStoresHashedLocalCode() {
        when(sender.localMode()).thenReturn(true);

        service.send("13800138000", "LOGIN", "127.0.0.1", "device-001");

        verify(sender).send("13800138000", "1234");
        verify(repository).save(any(VerificationCode.class));
    }

    @Test
    void rejectsSendingAgainInsideCooldown() {
        VerificationCode latest = new VerificationCode();
        latest.setCreatedAt(LocalDateTime.now());
        when(repository.findFirstByPhoneAndPurposeOrderByCreatedAtDesc("13800138000", "LOGIN"))
            .thenReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.send("13800138000", "LOGIN", "127.0.0.1", "device-001"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("发送过于频繁");
        verify(sender, never()).send(any(), any());
    }

    @Test
    void consumesCorrectCode() {
        when(sender.localMode()).thenReturn(true);
        service.send("13800138000", "LOGIN", "127.0.0.1", "device-001");
        org.mockito.ArgumentCaptor<VerificationCode> captor =
            org.mockito.ArgumentCaptor.forClass(VerificationCode.class);
        verify(repository).save(captor.capture());
        VerificationCode record = captor.getValue();
        when(repository.findFirstByPhoneAndPurposeAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            org.mockito.ArgumentMatchers.eq("13800138000"), org.mockito.ArgumentMatchers.eq("LOGIN"), any(LocalDateTime.class)
        )).thenReturn(Optional.of(record));

        service.verify("13800138000", "LOGIN", "1234");

        assertThat(record.getConsumedAt()).isNotNull();
        assertThat(record.getAttempts()).isZero();
    }

    @Test
    void countsWrongAttemptsWithoutConsumingImmediately() {
        when(sender.localMode()).thenReturn(true);
        service.send("13800138000", "LOGIN", "127.0.0.1", "device-001");
        org.mockito.ArgumentCaptor<VerificationCode> captor =
            org.mockito.ArgumentCaptor.forClass(VerificationCode.class);
        verify(repository).save(captor.capture());
        VerificationCode record = captor.getValue();
        when(repository.findFirstByPhoneAndPurposeAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            org.mockito.ArgumentMatchers.eq("13800138000"), org.mockito.ArgumentMatchers.eq("LOGIN"), any(LocalDateTime.class)
        )).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.verify("13800138000", "LOGIN", "0000"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("验证码不正确");

        assertThat(record.getAttempts()).isEqualTo(1);
        assertThat(record.getConsumedAt()).isNull();
    }
}

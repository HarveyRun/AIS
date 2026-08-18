package com.shixianwen.auth;

import com.shixianwen.common.BusinessException;
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
        service = new VerificationCodeService(repository, sender);
        ReflectionTestUtils.setField(service, "pepper", "test-pepper");
        ReflectionTestUtils.setField(service, "localCode", "1234");
        when(repository.findFirstByPhoneOrderByCreatedAtDesc("13800138000"))
            .thenReturn(Optional.empty());
    }

    @Test
    void sendsAndStoresHashedLocalCode() {
        when(sender.localMode()).thenReturn(true);

        service.send("13800138000", "127.0.0.1");

        verify(sender).send("13800138000", "1234");
        verify(repository).save(any(VerificationCode.class));
    }

    @Test
    void rejectsSendingAgainInsideCooldown() {
        VerificationCode latest = new VerificationCode();
        latest.setCreatedAt(LocalDateTime.now());
        when(repository.findFirstByPhoneOrderByCreatedAtDesc("13800138000"))
            .thenReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.send("13800138000", "127.0.0.1"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("发送过于频繁");
        verify(sender, never()).send(any(), any());
    }

    @Test
    void consumesCorrectCode() {
        when(sender.localMode()).thenReturn(true);
        service.send("13800138000", "127.0.0.1");
        org.mockito.ArgumentCaptor<VerificationCode> captor =
            org.mockito.ArgumentCaptor.forClass(VerificationCode.class);
        verify(repository).save(captor.capture());
        VerificationCode record = captor.getValue();
        when(repository.findFirstByPhoneAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            org.mockito.ArgumentMatchers.eq("13800138000"), any(LocalDateTime.class)
        )).thenReturn(Optional.of(record));

        service.verify("13800138000", "1234");

        assertThat(record.getConsumedAt()).isNotNull();
        assertThat(record.getAttempts()).isZero();
    }

    @Test
    void countsWrongAttemptsWithoutConsumingImmediately() {
        when(sender.localMode()).thenReturn(true);
        service.send("13800138000", "127.0.0.1");
        org.mockito.ArgumentCaptor<VerificationCode> captor =
            org.mockito.ArgumentCaptor.forClass(VerificationCode.class);
        verify(repository).save(captor.capture());
        VerificationCode record = captor.getValue();
        when(repository.findFirstByPhoneAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            org.mockito.ArgumentMatchers.eq("13800138000"), any(LocalDateTime.class)
        )).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.verify("13800138000", "0000"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("验证码不正确");

        assertThat(record.getAttempts()).isEqualTo(1);
        assertThat(record.getConsumedAt()).isNull();
    }
}

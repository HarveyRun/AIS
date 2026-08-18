package com.shixianwen.auth;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private AppTestLoginAccountService testAccountService;
    private VerificationCodeSender verificationCodeSender;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        testAccountService = mock(AppTestLoginAccountService.class);
        verificationCodeSender = mock(VerificationCodeSender.class);
        authService = new AuthService(
            mock(UserRepository.class),
            mock(AuthSessionRepository.class),
            mock(WalletAccountRepository.class),
            mock(UserLoginRecordRepository.class),
            testAccountService,
            verificationCodeSender,
            "1234",
            30
        );
    }

    @Test
    void appTestAccountSkipsSmsSender() {
        when(testAccountService.activeVerificationCode("19900000000"))
            .thenReturn(Optional.of("5678"));

        authService.sendVerificationCode("19900000000", true);

        verify(verificationCodeSender, never()).send("19900000000");
    }

    @Test
    void regularPhoneUsesSmsSenderBoundary() {
        when(testAccountService.activeVerificationCode("18800000000"))
            .thenReturn(Optional.empty());

        authService.sendVerificationCode("18800000000", true);

        verify(verificationCodeSender).send("18800000000");
    }

    @Test
    void appTestAccountUsesCurrentConfiguredCode() {
        when(testAccountService.activeVerificationCode("19900000000"))
            .thenReturn(Optional.of("5678"));

        assertDoesNotThrow(() -> authService.validateCode("19900000000", "5678", true));
        assertThrows(
            BusinessException.class,
            () -> authService.validateCode("19900000000", "1234", true)
        );
    }

    @Test
    void regularPhoneKeepsNormalVerificationRule() {
        when(testAccountService.activeVerificationCode("18800000000"))
            .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.validateCode("18800000000", "1234", true));
        assertThrows(
            BusinessException.class,
            () -> authService.validateCode("18800000000", "5678", true)
        );
    }

    @Test
    void webClientCannotUseAppTestAccountCode() {
        assertDoesNotThrow(() -> authService.validateCode("19900000000", "1234", false));
        assertThrows(
            BusinessException.class,
            () -> authService.validateCode("19900000000", "5678", false)
        );
        verify(testAccountService, never()).activeVerificationCode("19900000000");
    }

    @Test
    void webClientDoesNotBypassSmsSender() {
        authService.sendVerificationCode("19900000000", false);

        verify(verificationCodeSender).send("19900000000");
        verify(testAccountService, never()).activeVerificationCode("19900000000");
    }
}

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private AppTestLoginAccountService testAccountService;
    private VerificationCodeService verificationCodeService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        testAccountService = mock(AppTestLoginAccountService.class);
        verificationCodeService = mock(VerificationCodeService.class);
        authService = new AuthService(
            mock(UserRepository.class),
            mock(AuthSessionRepository.class),
            mock(WalletAccountRepository.class),
            mock(UserLoginRecordRepository.class),
            testAccountService,
            verificationCodeService,
            mock(UidAllocator.class),
            30
        );
    }

    @Test
    void appTestAccountSkipsSmsSender() {
        when(testAccountService.activeVerificationCode("19900000000"))
            .thenReturn(Optional.of("5678"));

        authService.sendVerificationCode("19900000000", "127.0.0.1", true);

        verify(verificationCodeService, never()).send("19900000000", "127.0.0.1");
    }

    @Test
    void regularPhoneUsesSmsSenderBoundary() {
        when(testAccountService.activeVerificationCode("18800000000"))
            .thenReturn(Optional.empty());

        authService.sendVerificationCode("18800000000", "127.0.0.1", true);

        verify(verificationCodeService).send("18800000000", "127.0.0.1");
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
        doThrow(BusinessException.badRequest("验证码不正确"))
            .when(verificationCodeService).verify("18800000000", "5678");

        assertDoesNotThrow(() -> authService.validateCode("18800000000", "1234", true));
        assertThrows(
            BusinessException.class,
            () -> authService.validateCode("18800000000", "5678", true)
        );
    }

    @Test
    void webClientCannotUseAppTestAccountCode() {
        doThrow(BusinessException.badRequest("验证码不正确"))
            .when(verificationCodeService).verify("19900000000", "5678");
        assertDoesNotThrow(() -> authService.validateCode("19900000000", "1234", false));
        assertThrows(
            BusinessException.class,
            () -> authService.validateCode("19900000000", "5678", false)
        );
        verify(testAccountService, never()).activeVerificationCode("19900000000");
    }

    @Test
    void webClientDoesNotBypassSmsSender() {
        authService.sendVerificationCode("19900000000", "127.0.0.1", false);

        verify(verificationCodeService).send("19900000000", "127.0.0.1");
        verify(testAccountService, never()).activeVerificationCode("19900000000");
    }
}

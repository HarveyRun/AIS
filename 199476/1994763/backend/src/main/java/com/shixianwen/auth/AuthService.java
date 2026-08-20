package com.shixianwen.auth;

import com.shixianwen.common.BusinessException;
import com.shixianwen.network.ClientNetworkInfo;
import com.shixianwen.security.LoginAttemptService;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletAccount;
import com.shixianwen.wallet.WalletAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final UserLoginRecordRepository loginRecordRepository;
    private final AppTestLoginAccountService appTestLoginAccountService;
    private final VerificationCodeService verificationCodeService;
    private final UidAllocator uidAllocator;
    private final LoginAttemptService loginAttempts;
    private final SecurityEventService securityEvents;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int tokenValidDays;

    public AuthService(
        UserRepository userRepository,
        AuthSessionRepository authSessionRepository,
        WalletAccountRepository walletAccountRepository,
        UserLoginRecordRepository loginRecordRepository,
        AppTestLoginAccountService appTestLoginAccountService,
        VerificationCodeService verificationCodeService,
        UidAllocator uidAllocator,
        LoginAttemptService loginAttempts,
        SecurityEventService securityEvents,
        @Value("${app.auth.token-valid-days}") int tokenValidDays
    ) {
        this.userRepository = userRepository;
        this.authSessionRepository = authSessionRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.loginRecordRepository = loginRecordRepository;
        this.appTestLoginAccountService = appTestLoginAccountService;
        this.verificationCodeService = verificationCodeService;
        this.uidAllocator = uidAllocator;
        this.loginAttempts = loginAttempts;
        this.securityEvents = securityEvents;
        this.tokenValidDays = tokenValidDays;
    }

    public void sendVerificationCode(String phone, String requestIp, String deviceId, boolean appClient) {
        if (appClient && appTestLoginAccountService.activeVerificationCode(phone).isPresent()) {
            return;
        }
        verificationCodeService.send(phone, "LOGIN", requestIp, deviceId);
    }

    public boolean validateCode(String phone, String code, boolean appClient) {
        if (appClient) {
            var appTestCode = appTestLoginAccountService.activeVerificationCode(phone);
            if (appTestCode.isPresent()) {
                if (!appTestCode.get().equals(code)) {
                    throw BusinessException.badRequest("验证码不正确");
                }
                return true;
            }
        }
        verificationCodeService.verify(phone, "LOGIN", code);
        return false;
    }

    @Transactional
    public LoginResult login(
        String phone,
        String code,
        ClientNetworkInfo network,
        String deviceId,
        boolean appClient
    ) {
        String safeDevice = LoginAttemptService.safeDevice(deviceId);
        loginAttempts.requireAllowed("USER", phone, network.ipAddress(), safeDevice);

        final boolean testLogin;
        try {
            testLogin = validateCode(phone, code, appClient);
        } catch (BusinessException exception) {
            loginAttempts.record("USER", phone, network.ipAddress(), safeDevice, false);
            securityEvents.recordSafely(
                null, null, "USER_LOGIN_FAILED", "MEDIUM", network.ipAddress(), safeDevice,
                "验证码校验失败"
            );
            throw exception;
        }

        var existingUser = userRepository.findByPhone(phone);
        boolean newlyCreated = existingUser.isEmpty();
        User user = existingUser.orElseGet(() -> createUser(phone, network));
        if (testLogin) {
            user.setAccountType("TEST");
        } else if ("TEST".equals(user.getAccountType())) {
            throw BusinessException.forbidden("测试账号不能进入真实业务");
        }
        ensureAccountAvailable(user);
        user.setLastLoginIp(network.ipAddress());
        user.setLastLoginLocation(network.location());
        user.setLastLoginAt(LocalDateTime.now());

        UserLoginRecord loginRecord = new UserLoginRecord();
        loginRecord.setUser(user);
        loginRecord.setIpAddress(network.ipAddress());
        loginRecord.setIpLocation(network.location());
        loginRecord.setDeviceId(safeDevice);
        loginRecordRepository.save(loginRecord);
        loginAttempts.record("USER", phone, network.ipAddress(), safeDevice, true);
        securityEvents.recordSafely(
            newlyCreated ? null : user.getId(), null,
            testLogin ? "TEST_ACCOUNT_LOGIN" : "USER_LOGIN_SUCCESS",
            testLogin ? "MEDIUM" : "INFO", network.ipAddress(), safeDevice,
            newlyCreated ? "newUid=" + user.getUid() : null
        );
        return createSession(user);
    }

    private User createUser(String phone, ClientNetworkInfo network) {
        User user = new User();
        user.setUid(uidAllocator.allocate());
        user.setPhone(phone);
        user.setRegisterIp(network.ipAddress());
        user.setRegisterLocation(network.location());
        user = userRepository.save(user);

        WalletAccount wallet = new WalletAccount();
        wallet.setUser(user);
        walletAccountRepository.save(wallet);
        return user;
    }

    @Transactional
    public User authenticate(String rawToken) {
        AuthSession session = authSessionRepository
            .findByTokenHashAndExpiresAtAfter(hash(rawToken), LocalDateTime.now())
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录"));
        User user = session.getUser();
        ensureAccountAvailable(user);
        return user;
    }

    @Transactional
    public void logout(String rawToken) {
        authSessionRepository.deleteByTokenHash(hash(rawToken));
    }

    private LoginResult createSession(User user) {
        authSessionRepository.deleteByUserId(user.getId());
        authSessionRepository.flush();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);

        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setTokenHash(hash(token));
        session.setExpiresAt(LocalDateTime.now().plusDays(tokenValidDays));
        authSessionRepository.save(session);
        return new LoginResult(token, UserView.from(user));
    }

    private void ensureAccountAvailable(User user) {
        if ("ACTIVE".equals(user.getAccountStatus())) return;

        LocalDateTime now = LocalDateTime.now();
        if (user.getBanUntil() != null && !user.getBanUntil().isAfter(now)) {
            user.setAccountStatus("ACTIVE");
            user.setBanReason(null);
            user.setBannedAt(null);
            user.setBanUntil(null);
            user.setBannedByAdmin(null);
            userRepository.save(user);
            return;
        }

        String reason = user.getBanReason();
        if (reason == null || reason.isBlank()) reason = "违反平台规则";
        throw new AccountPenaltyException(reason, user.getBanUntil());
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record LoginResult(String token, UserView user) {
    }

    public record UserView(
        Long id,
        String uid,
        String phone,
        String nickname,
        String avatarUrl,
        boolean acceptingInquiries,
        LocalDateTime acceptingInquiriesUpdatedAt,
        int inquiryPriceMin,
        int inquiryPriceMax,
        LocalDateTime inquiryPriceUpdatedAt,
        String answererStatus
    ) {
        public static UserView from(User user) {
            return new UserView(
                user.getId(), user.getUid(), user.getPhone(), user.getNickname(), user.getAvatarUrl(),
                user.isAcceptingInquiries(), user.getAcceptingInquiriesUpdatedAt(),
                user.getInquiryPriceMin(), user.getInquiryPriceMax(), user.getInquiryPriceUpdatedAt(),
                user.getAnswererStatus()
            );
        }
    }
}

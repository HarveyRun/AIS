package com.shixianwen.auth;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletAccount;
import com.shixianwen.wallet.WalletAccountRepository;
import com.shixianwen.network.ClientNetworkInfo;
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
        @Value("${app.auth.token-valid-days}") int tokenValidDays
    ) {
        this.userRepository = userRepository;
        this.authSessionRepository = authSessionRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.loginRecordRepository = loginRecordRepository;
        this.appTestLoginAccountService = appTestLoginAccountService;
        this.verificationCodeService = verificationCodeService;
        this.uidAllocator = uidAllocator;
        this.tokenValidDays = tokenValidDays;
    }

    public void sendVerificationCode(String phone, String requestIp, boolean appClient) {
        if (appClient && appTestLoginAccountService.activeVerificationCode(phone).isPresent()) {
            return;
        }
        verificationCodeService.send(phone, requestIp);
    }

    public void validateCode(String phone, String code, boolean appClient) {
        if (appClient) {
            var appTestCode = appTestLoginAccountService.activeVerificationCode(phone);
            if (appTestCode.isPresent()) {
                if (!appTestCode.get().equals(code)) {
                    throw BusinessException.badRequest("验证码不正确");
                }
                return;
            }
        }
        verificationCodeService.verify(phone, code);
    }

    @Transactional
    public LoginResult login(String phone, String code, ClientNetworkInfo network, boolean appClient) {
        validateCode(phone, code, appClient);
        User user = userRepository.findByPhone(phone).orElseGet(() -> createUser(phone, network));
        ensureAccountAvailable(user);
        user.setLastLoginIp(network.ipAddress());
        user.setLastLoginLocation(network.location());
        user.setLastLoginAt(LocalDateTime.now());
        UserLoginRecord loginRecord = new UserLoginRecord();
        loginRecord.setUser(user);
        loginRecord.setIpAddress(network.ipAddress());
        loginRecord.setIpLocation(network.location());
        loginRecordRepository.save(loginRecord);
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
            .orElseThrow(() -> new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "登录已失效，请重新登录"
            ));
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

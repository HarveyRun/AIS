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
    private final VerificationCodeSender verificationCodeSender;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String verificationCode;
    private final int tokenValidDays;

    public AuthService(
        UserRepository userRepository,
        AuthSessionRepository authSessionRepository,
        WalletAccountRepository walletAccountRepository,
        UserLoginRecordRepository loginRecordRepository,
        AppTestLoginAccountService appTestLoginAccountService,
        VerificationCodeSender verificationCodeSender,
        @Value("${app.auth.demo-verification-code}") String verificationCode,
        @Value("${app.auth.token-valid-days}") int tokenValidDays
    ) {
        this.userRepository = userRepository;
        this.authSessionRepository = authSessionRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.loginRecordRepository = loginRecordRepository;
        this.appTestLoginAccountService = appTestLoginAccountService;
        this.verificationCodeSender = verificationCodeSender;
        this.verificationCode = verificationCode;
        this.tokenValidDays = tokenValidDays;
    }

    public void sendVerificationCode(String phone, boolean appClient) {
        if (appClient && appTestLoginAccountService.activeVerificationCode(phone).isPresent()) {
            return;
        }
        verificationCodeSender.send(phone);
    }

    public void validateCode(String phone, String code, boolean appClient) {
        String expectedCode = appClient
            ? appTestLoginAccountService.activeVerificationCode(phone).orElse(verificationCode)
            : verificationCode;
        if (!expectedCode.equals(code)) {
            throw BusinessException.badRequest("验证码不正确");
        }
    }

    @Transactional
    public LoginResult login(String phone, String code, ClientNetworkInfo network, boolean appClient) {
        validateCode(phone, code, appClient);
        User user = userRepository.findByPhone(phone).orElseGet(() -> createUser(phone, network));
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw BusinessException.forbidden("该账号当前无法登录");
        }
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
        user.setUid(generateUid());
        user.setPhone(phone);
        user.setRegisterIp(network.ipAddress());
        user.setRegisterLocation(network.location());
        user = userRepository.save(user);

        WalletAccount wallet = new WalletAccount();
        wallet.setUser(user);
        walletAccountRepository.save(wallet);
        return user;
    }

    @Transactional(readOnly = true)
    public User authenticate(String rawToken) {
        return authSessionRepository.findByTokenHashAndExpiresAtAfter(hash(rawToken), LocalDateTime.now())
            .map(AuthSession::getUser)
            .filter(user -> "ACTIVE".equals(user.getAccountStatus()))
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录"));
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

    private String generateUid() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String uid = String.valueOf(1_000_000 + secureRandom.nextInt(9_000_000));
            if (userRepository.findByUidAndAccountStatus(uid, "ACTIVE").isEmpty()) {
                return uid;
            }
        }
        throw new IllegalStateException("无法生成用户UID");
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
        String answererStatus
    ) {
        public static UserView from(User user) {
            return new UserView(
                user.getId(), user.getUid(), user.getPhone(), user.getNickname(), user.getAvatarUrl(),
                user.isAcceptingInquiries(), user.getAnswererStatus()
            );
        }
    }
}

package com.shixianwen.auth;

import com.shixianwen.common.BusinessException;
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
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String verificationCode;
    private final int tokenValidDays;

    public AuthService(
        UserRepository userRepository,
        AuthSessionRepository authSessionRepository,
        WalletAccountRepository walletAccountRepository,
        @Value("${app.auth.demo-verification-code}") String verificationCode,
        @Value("${app.auth.token-valid-days}") int tokenValidDays
    ) {
        this.userRepository = userRepository;
        this.authSessionRepository = authSessionRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.verificationCode = verificationCode;
        this.tokenValidDays = tokenValidDays;
    }

    public void validateCode(String code) {
        if (!verificationCode.equals(code)) {
            throw BusinessException.badRequest("验证码不正确");
        }
    }

    @Transactional
    public LoginResult register(String phone, String nickname, String code) {
        validateCode(code);
        if (userRepository.findByPhoneAndAccountStatus(phone, "ACTIVE").isPresent()) {
            throw BusinessException.badRequest("该手机号已经注册");
        }

        User user = new User();
        user.setUid(generateUid());
        user.setPhone(phone);
        user.setNickname(normalizeNickname(nickname));
        user = userRepository.save(user);

        WalletAccount wallet = new WalletAccount();
        wallet.setUser(user);
        walletAccountRepository.save(wallet);
        return createSession(user);
    }

    @Transactional
    public LoginResult login(String phone, String code) {
        validateCode(code);
        User user = userRepository.findByPhoneAndAccountStatus(phone, "ACTIVE")
            .orElseThrow(() -> BusinessException.notFound("账号不存在，请先注册"));
        return createSession(user);
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

    private static String normalizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) return null;
        return nickname.trim();
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

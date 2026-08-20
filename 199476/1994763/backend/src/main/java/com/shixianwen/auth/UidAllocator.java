package com.shixianwen.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Component
public class UidAllocator {
    private static final int UID_MIN = 1_000_000;
    private static final int UID_RANGE = 9_000_000;
    private static final int MAX_ATTEMPTS = 30;

    private final JdbcTemplate jdbc;
    private final SecureRandom random;

    @Autowired
    public UidAllocator(JdbcTemplate jdbc) {
        this(jdbc, new SecureRandom());
    }

    UidAllocator(JdbcTemplate jdbc, SecureRandom random) {
        this.jdbc = jdbc;
        this.random = random;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String allocate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String uid = String.valueOf(UID_MIN + random.nextInt(UID_RANGE));
            int reserved = jdbc.update(
                "INSERT IGNORE INTO uid_reservations(uid) " +
                    "SELECT ? WHERE NOT EXISTS (SELECT 1 FROM users WHERE uid = ?)",
                uid,
                uid
            );
            if (reserved == 1) return uid;
        }
        throw new IllegalStateException("无法生成用户UID");
    }
}

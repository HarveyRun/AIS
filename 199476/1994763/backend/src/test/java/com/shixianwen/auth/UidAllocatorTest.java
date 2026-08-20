package com.shixianwen.auth;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UidAllocatorTest {
    @Test
    void retriesWhenGeneratedUidHasAlreadyBeenReserved() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SecureRandom random = mock(SecureRandom.class);
        when(random.nextInt(9_000_000)).thenReturn(12, 34);
        when(jdbc.update(
            "INSERT IGNORE INTO uid_reservations(uid) " +
                "SELECT ? WHERE NOT EXISTS (SELECT 1 FROM users WHERE uid = ?)",
            "1000012",
            "1000012"
        )).thenReturn(0);
        when(jdbc.update(
            "INSERT IGNORE INTO uid_reservations(uid) " +
                "SELECT ? WHERE NOT EXISTS (SELECT 1 FROM users WHERE uid = ?)",
            "1000034",
            "1000034"
        )).thenReturn(1);

        UidAllocator allocator = new UidAllocator(jdbc, random);

        assertEquals("1000034", allocator.allocate());
        verify(random, times(2)).nextInt(9_000_000);
    }

    @Test
    void failsAfterAllCandidatesConflict() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SecureRandom random = mock(SecureRandom.class);
        when(random.nextInt(9_000_000)).thenReturn(7);
        when(jdbc.update(
            "INSERT IGNORE INTO uid_reservations(uid) " +
                "SELECT ? WHERE NOT EXISTS (SELECT 1 FROM users WHERE uid = ?)",
            "1000007",
            "1000007"
        )).thenReturn(0);

        UidAllocator allocator = new UidAllocator(jdbc, random);

        assertThrows(IllegalStateException.class, allocator::allocate);
    }
}

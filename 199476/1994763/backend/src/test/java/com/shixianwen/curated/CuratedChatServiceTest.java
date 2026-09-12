package com.shixianwen.curated;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CuratedChatServiceTest {
    @Test
    void formatsConfiguredMembershipTerms() {
        BigDecimal price = new BigDecimal("99.00");
        assertEquals("99元/月", CuratedChatService.membershipPriceText(price, 1));
        assertEquals("99元/半年", CuratedChatService.membershipPriceText(price, 6));
        assertEquals("99元/年", CuratedChatService.membershipPriceText(price, 12));
        assertEquals("99元/永久", CuratedChatService.membershipPriceText(price, 1200));
        assertEquals("99元/3个月", CuratedChatService.membershipPriceText(price, 3));
        assertEquals(
            "128.5元/3个月",
            CuratedChatService.membershipPriceText(new BigDecimal("128.50"), 3)
        );
    }

    @Test
    void calculatesFiniteAndPermanentTermsOnServer() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 1, 31, 10, 20);
        assertEquals(
            LocalDateTime.of(2026, 2, 28, 10, 20),
            CuratedChatService.membershipExpiresAt(startsAt, 1)
        );
        assertEquals(
            LocalDateTime.of(9999, 12, 31, 23, 59, 59),
            CuratedChatService.membershipExpiresAt(startsAt, 1200)
        );
    }
}

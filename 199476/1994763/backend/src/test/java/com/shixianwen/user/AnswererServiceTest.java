package com.shixianwen.user;

import com.shixianwen.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnswererServiceTest {
    @Test
    void convertsHomepageFiltersToStoredExperienceTypes() {
        assertEquals("", AnswererService.businessTypeFilter(null));
        assertEquals("", AnswererService.businessTypeFilter("ALL"));
        assertEquals("PUBLIC_WELFARE", AnswererService.businessTypeFilter("free"));
        assertEquals("MONETIZED", AnswererService.businessTypeFilter("PAID"));
    }

    @Test
    void rejectsUnknownHomepageFilter() {
        assertThrows(
            BusinessException.class,
            () -> AnswererService.businessTypeFilter("UNKNOWN")
        );
    }
}

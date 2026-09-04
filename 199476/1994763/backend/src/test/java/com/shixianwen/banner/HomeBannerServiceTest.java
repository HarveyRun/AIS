package com.shixianwen.banner;

import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import com.shixianwen.storage.FileStorage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeBannerServiceTest {
    @Test
    void publicListOnlyUsesTheScheduledRepositoryQuery() {
        Fixture fixture = fixture();
        HomeBanner banner = banner(
            true,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        );
        when(fixture.repository
            .findAllByDeletedFalseAndEnabledTrueAndStartAtLessThanEqualAndEndAtGreaterThanOrderBySortOrderAscIdAsc(
                any(LocalDateTime.class),
                any(LocalDateTime.class)
            )).thenReturn(List.of(banner));

        List<HomeBannerService.PublicBannerView> result = fixture.service.publicBanners();

        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).id());
    }

    @Test
    void availabilityReturnsTheLatestBannerWhenItIsCurrentlyActive() {
        Fixture fixture = fixture();
        HomeBanner banner = banner(
            true,
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().plusMinutes(1)
        );
        when(fixture.repository.findById(9L)).thenReturn(Optional.of(banner));

        HomeBannerService.BannerAvailabilityView result = fixture.service.availability(9L);

        assertTrue(result.available());
        assertEquals("AVAILABLE", result.state());
        assertEquals("MY_EXPERIENCES", result.banner().actionType());
    }

    @Test
    void availabilityRejectsFutureExpiredAndDisabledBanners() {
        Fixture fixture = fixture();
        LocalDateTime now = LocalDateTime.now();
        HomeBanner future = banner(true, now.plusMinutes(1), now.plusMinutes(2));
        HomeBanner expired = banner(true, now.minusMinutes(2), now.minusMinutes(1));
        HomeBanner disabled = banner(false, now.minusMinutes(1), now.plusMinutes(1));
        when(fixture.repository.findById(1L)).thenReturn(Optional.of(future));
        when(fixture.repository.findById(2L)).thenReturn(Optional.of(expired));
        when(fixture.repository.findById(3L)).thenReturn(Optional.of(disabled));

        HomeBannerService.BannerAvailabilityView notStarted = fixture.service.availability(1L);
        HomeBannerService.BannerAvailabilityView ended = fixture.service.availability(2L);
        HomeBannerService.BannerAvailabilityView offline = fixture.service.availability(3L);

        assertFalse(notStarted.available());
        assertEquals("NOT_STARTED", notStarted.state());
        assertEquals("ENDED", ended.state());
        assertEquals("OFFLINE", offline.state());
    }

    @Test
    void createRejectsAnInvalidSchedule() {
        Fixture fixture = fixture();
        LocalDateTime startAt = LocalDateTime.now().plusDays(2);
        HomeBannerService.SaveCommand command = new HomeBannerService.SaveCommand(
            "TEXT_ONLY",
            null,
            "测试Banner",
            null,
            null,
            "NONE",
            10,
            startAt,
            startAt.minusMinutes(1),
            true
        );

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> fixture.service.create(new AdminUser(), command, "127.0.0.1")
        );

        assertEquals("结束时间必须晚于开始时间", error.getMessage());
    }

    private Fixture fixture() {
        HomeBannerRepository repository = mock(HomeBannerRepository.class);
        HomeBannerService service = new HomeBannerService(
            repository,
            mock(AdminAuditLogRepository.class),
            mock(FileStorage.class)
        );
        return new Fixture(service, repository);
    }

    private HomeBanner banner(
        boolean enabled,
        LocalDateTime startAt,
        LocalDateTime endAt
    ) {
        HomeBanner banner = new HomeBanner();
        banner.setId(9L);
        banner.setDisplayMode("TEXT_ONLY");
        banner.setTitle("测试Banner");
        banner.setActionType("MY_EXPERIENCES");
        banner.setSortOrder(10);
        banner.setStartAt(startAt);
        banner.setEndAt(endAt);
        banner.setEnabled(enabled);
        return banner;
    }

    private record Fixture(HomeBannerService service, HomeBannerRepository repository) {
    }
}

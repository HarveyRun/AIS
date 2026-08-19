package com.shixianwen.version;

import com.shixianwen.admin.AdminAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppVersionServiceTest {
    private AppVersionRepository repository;
    private AppVersionService service;

    @BeforeEach
    void setUp() {
        repository = mock(AppVersionRepository.class);
        service = new AppVersionService(
            repository,
            mock(AdminAuditLogRepository.class)
        );
    }

    @Test
    void updateBelowLatestButWithinMinimumIsOptional() {
        when(repository.findFirstByPlatformAndPublishedTrueAndDeletedFalseOrderByVersionCodeDesc(
            "ANDROID"
        )).thenReturn(Optional.of(version(6, 4)));

        AppVersionService.UpdateCheck result = service.check("android", 5);

        assertTrue(result.hasUpdate());
        assertFalse(result.forceUpdate());
    }

    @Test
    void updateBelowMinimumIsForced() {
        when(repository.findFirstByPlatformAndPublishedTrueAndDeletedFalseOrderByVersionCodeDesc(
            "ANDROID"
        )).thenReturn(Optional.of(version(6, 6)));

        AppVersionService.UpdateCheck result = service.check("ANDROID", 5);

        assertTrue(result.hasUpdate());
        assertTrue(result.forceUpdate());
    }

    @Test
    void latestVersionDoesNotReceiveAnUpdate() {
        when(repository.findFirstByPlatformAndPublishedTrueAndDeletedFalseOrderByVersionCodeDesc(
            "ANDROID"
        )).thenReturn(Optional.of(version(6, 6)));

        AppVersionService.UpdateCheck result = service.check("ANDROID", 6);

        assertFalse(result.hasUpdate());
        assertFalse(result.forceUpdate());
    }

    private AppVersion version(int versionCode, int minimumVersionCode) {
        AppVersion version = new AppVersion();
        version.setPlatform("ANDROID");
        version.setVersionName("1.0." + versionCode);
        version.setVersionCode(versionCode);
        version.setMinimumSupportedVersionCode(minimumVersionCode);
        version.setTitle("发现新版本");
        version.setUpdateContent("修复已知问题");
        version.setDownloadUrl("https://example.com/app.apk");
        version.setPublished(true);
        return version;
    }
}

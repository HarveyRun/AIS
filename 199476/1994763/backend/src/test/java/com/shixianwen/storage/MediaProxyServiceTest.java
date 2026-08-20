package com.shixianwen.storage;

import com.shixianwen.common.BusinessException;
import com.shixianwen.integration.ThirdPartySettings;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaProxyServiceTest {
    @Test
    void rejectsUrlsOutsideConfiguredOssDomain() {
        ThirdPartySettings settings = mock(ThirdPartySettings.class);
        when(settings.value("app.storage.oss.public-domain", "oss.domain"))
            .thenReturn("https://bucket.oss-cn-beijing.aliyuncs.com");
        MediaProxyService service = new MediaProxyService(settings, new FileTypeDetector());

        assertThatThrownBy(() -> service.loadImage("http://127.0.0.1/internal"))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
    }
}

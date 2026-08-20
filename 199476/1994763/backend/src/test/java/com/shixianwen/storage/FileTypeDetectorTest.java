package com.shixianwen.storage;

import com.shixianwen.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileTypeDetectorTest {
    private final FileTypeDetector detector = new FileTypeDetector();

    @Test
    void detectsImageFromBytesInsteadOfDeclaredMimeType() {
        byte[] png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
        );
        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.exe", "application/x-msdownload", png
        );

        FileTypeDetector.DetectedFile result = detector.requireImage(file);

        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.extension()).isEqualTo(".png");
    }

    @Test
    void rejectsExecutableRenamedAsImage() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.png", "image/png", new byte[] {'M', 'Z', 0, 0, 0}
        );

        assertThatThrownBy(() -> detector.requireImage(file))
            .isInstanceOf(BusinessException.class);
    }
}

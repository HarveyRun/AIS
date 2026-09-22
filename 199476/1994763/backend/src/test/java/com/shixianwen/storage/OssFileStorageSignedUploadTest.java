package com.shixianwen.storage;

import com.shixianwen.integration.ThirdPartySettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OssFileStorageSignedUploadTest {
    @Test
    void signedPartUrlTargetsPrivateBucketAndSpecificMultipartPart() {
        ThirdPartySettings settings = mock(ThirdPartySettings.class);
        when(settings.value("app.storage.oss.endpoint", "oss.endpoint"))
            .thenReturn("https://oss-cn-beijing.aliyuncs.com");
        when(settings.value("app.storage.oss.access-key-id", "oss.accessKeyId"))
            .thenReturn("test-key-id");
        when(settings.value("app.storage.oss.access-key-secret", "oss.accessKeySecret"))
            .thenReturn("test-key-secret");
        when(settings.value("app.storage.oss.public-bucket"))
            .thenReturn("public-bucket");
        when(settings.value("app.storage.oss.private-bucket"))
            .thenReturn("private-bucket");
        when(settings.value("app.storage.oss.public-domain", "oss.domain"))
            .thenReturn("https://public.example.com");
        OssFileStorage storage = new OssFileStorage(settings, mock(FileTypeDetector.class));
        try {
            String url = storage.signPrivatePart("private/proof.zip", "upload-id", 2);
            assertTrue(url.startsWith("https://private-bucket.oss-cn-beijing.aliyuncs.com/"));
            assertTrue(url.contains("partNumber=2"));
            assertTrue(url.contains("uploadId=upload-id"));
            assertTrue(url.contains("Signature="));
        } finally {
            storage.close();
        }
    }
}

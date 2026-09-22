package com.shixianwen.certification;

import com.aliyun.oss.model.PartSummary;
import com.shixianwen.config.AppGlobalSettingService;
import com.shixianwen.storage.OssFileStorage;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExperienceProofUploadServiceTest {
    private final ExperienceProofUploadRepository uploads = mock(ExperienceProofUploadRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OssFileStorage oss = mock(OssFileStorage.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<OssFileStorage> ossProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<AppGlobalSettingService> settingsProvider = mock(ObjectProvider.class);
    private ExperienceProofUploadService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ExperienceProofUploadService(uploads, users, ossProvider, settingsProvider);
        user = new User();
        user.setId(7L);
        user.setUid("1234567");
        when(ossProvider.getIfAvailable()).thenReturn(oss);
        when(users.findWithLockById(7L)).thenReturn(Optional.of(user));
        when(uploads.save(any(ExperienceProofUpload.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void initiationCreatesPrivateMultipartWithoutReceivingFileBytes() {
        when(oss.initiatePrivateMultipart(anyString(), eq("application/zip"))).thenReturn("oss-id");
        var result = service.initiate(user, "proof.zip", 9L * 1024 * 1024);
        assertEquals(8 * 1024 * 1024, result.partSize());
        assertEquals(2, result.partCount());
        verify(oss).initiatePrivateMultipart(
            matches("private/certifications/1234567/direct/.+\\.zip"), eq("application/zip")
        );
    }

    @Test
    void completedUploadChecksAllPartSizesAndMagicBytesBeforeBinding() {
        var upload = uploading(9L * 1024 * 1024);
        when(uploads.findByIdAndUserId("token", 7L)).thenReturn(Optional.of(upload));
        when(oss.privateParts(upload.getStorageKey(), upload.getOssUploadId()))
            .thenReturn(List.of(part(1, 8L * 1024 * 1024), part(2, 1024 * 1024)));
        when(oss.privateObjectSize(upload.getStorageKey())).thenReturn(upload.getExpectedSize());
        when(oss.privateObjectHeader(upload.getStorageKey()))
            .thenReturn(new byte[] {0x50, 0x4b, 0x03, 0x04, 0, 0, 0, 0});

        assertEquals("COMPLETE", service.complete(user, "token").status());
        assertEquals("PROOF.zip", service.consume(user, "token").originalName());
        assertEquals("CONSUMED", upload.getStatus());
        assertThrows(RuntimeException.class, () -> service.consume(user, "token"));
    }

    @Test
    void missingPartCannotComplete() {
        var upload = uploading(9L * 1024 * 1024);
        when(uploads.findByIdAndUserId("token", 7L)).thenReturn(Optional.of(upload));
        when(oss.privateParts(upload.getStorageKey(), upload.getOssUploadId()))
            .thenReturn(List.of(part(1, 8L * 1024 * 1024)));
        assertThrows(RuntimeException.class, () -> service.complete(user, "token"));
        verify(oss, never()).completePrivateMultipart(anyString(), anyString(), anyList());
    }

    @Test
    void forgedFileHeaderIsRemoved() {
        var upload = uploading(1024);
        when(uploads.findByIdAndUserId("token", 7L)).thenReturn(Optional.of(upload));
        when(oss.privateParts(upload.getStorageKey(), upload.getOssUploadId()))
            .thenReturn(List.of(part(1, 1024)));
        when(oss.privateObjectSize(upload.getStorageKey())).thenReturn(1024L);
        when(oss.privateObjectHeader(upload.getStorageKey()))
            .thenReturn(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
        assertThrows(RuntimeException.class, () -> service.complete(user, "token"));
        verify(oss).deletePrivateObject(upload.getStorageKey());
        assertEquals("INVALID", upload.getStatus());
    }

    @Test
    void anotherUserCannotSignOrConsumeUpload() {
        assertThrows(RuntimeException.class, () -> service.signPart(user, "other", 1));
        assertThrows(RuntimeException.class, () -> service.consume(user, "other"));
        verifyNoInteractions(oss);
    }

    private ExperienceProofUpload uploading(long size) {
        var upload = new ExperienceProofUpload();
        upload.setId("token");
        upload.setUserId(7L);
        upload.setStorageKey("private/certifications/1234567/direct/token.zip");
        upload.setOssUploadId("oss-id");
        upload.setOriginalName("PROOF.zip");
        upload.setContentType("application/zip");
        upload.setExpectedSize(size);
        upload.setPartSize(8 * 1024 * 1024);
        upload.setStatus("UPLOADING");
        upload.setExpiresAt(LocalDateTime.now().plusHours(1));
        return upload;
    }

    private PartSummary part(int number, long size) {
        PartSummary part = new PartSummary();
        part.setPartNumber(number);
        part.setSize(size);
        part.setETag("etag-" + number);
        return part;
    }
}

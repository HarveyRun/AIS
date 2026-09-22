package com.shixianwen.certification;

import com.shixianwen.analytics.AnalyticsEventService;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.FileTypeDetector;
import com.shixianwen.storage.StoredFile;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CertificationDirectProofSubmissionTest {
    @Test
    void submitsOnlyUploadTokenAndAttachesAlreadyUploadedPrivateObject() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        FileStorage storage = mock(FileStorage.class);
        SensitiveWordService words = mock(SensitiveWordService.class);
        ExperienceProofUploadService uploads = mock(ExperienceProofUploadService.class);
        CertificationService service = new CertificationService(
            certifications, users, storage, words, mock(FileTypeDetector.class),
            mock(AnalyticsEventService.class)
        );
        ReflectionTestUtils.setField(service, "proofUploads", uploads);
        User user = new User();
        user.setId(5L);
        user.setUid("1234567");
        when(users.findWithLockById(5L)).thenReturn(Optional.of(user));
        when(words.mask(any(String.class))).thenAnswer(call -> call.getArgument(0));
        when(certifications.save(any(Certification.class))).thenAnswer(call -> call.getArgument(0));
        when(uploads.consume(user, "token")).thenReturn(new ExperienceProofUploadService.BoundProof(
            new StoredFile("private/certifications/1234567/direct/file.zip", null, "application/zip", 1024),
            "proof.zip"
        ));

        var result = service.submitExperience(
            user, null, "我的装修经历", "我经历过一次装修", null, null, null, null,
            null, null, null, null, false, null, null, List.of(), "ANDROID", "token"
        );

        assertEquals(1, result.materials().size());
        assertEquals("PROOF_ARCHIVE", result.materials().get(0).kind());
        verify(uploads).consume(user, "token");
        verify(storage, never()).store(any(org.springframework.web.multipart.MultipartFile.class), anyString(), any());
    }
}

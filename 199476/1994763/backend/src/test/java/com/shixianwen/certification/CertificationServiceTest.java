package com.shixianwen.certification;

import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.storage.StoredFile;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CertificationServiceTest {
    @Test
    void identityApprovalAloneAllowsSubmittingExperience() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        FileStorage storage = mock(FileStorage.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        CertificationService service = new CertificationService(
            certifications,
            mock(UserRepository.class),
            storage,
            sensitiveWords
        );
        User user = new User();
        user.setId(7L);
        user.setUid("7996702");
        MockMultipartFile archive = new MockMultipartFile(
            "files",
            "proof.zip",
            "application/zip",
            new byte[]{1}
        );
        when(certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            7L,
            "IDENTITY",
            "APPROVED"
        )).thenReturn(true);
        when(certifications.findFirstByUserIdAndCertificationTypeOrderByIdDesc(7L, "EXPERIENCE"))
            .thenReturn(Optional.empty());
        when(sensitiveWords.mask(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.store(any(), any(String.class), any(StorageVisibility.class)))
            .thenReturn(new StoredFile("certifications/proof.zip", null, "application/zip", 1));
        when(storage.accessUrl("certifications/proof.zip", StorageVisibility.PRIVATE))
            .thenReturn("https://signed.example.com/proof.zip");
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CertificationService.CertificationView result = service.submitExperience(
            user,
            null,
            "经历标题",
            "经历简述",
            null,
            List.of(archive)
        );

        assertEquals("经历标题", result.title());
        assertEquals(1, result.materials().size());
        verify(certifications, never()).existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            7L,
            "MAIN_JOB",
            "APPROVED"
        );
    }
}

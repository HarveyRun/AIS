package com.shixianwen.certification;

import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.storage.StoredFile;
import com.shixianwen.storage.FileTypeDetector;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CertificationServiceTest {
    @Test
    void pendingExperienceDoesNotExposeProofDownloadUrls() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        FileStorage storage = mock(FileStorage.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            storage,
            mock(SensitiveWordService.class),
            mock(FileTypeDetector.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(7L);
        Certification pending = certification(user, "EXPERIENCE");
        pending.setCategory("EXPERIENCE");
        pending.setStatus("PENDING");
        CertificationMaterial proof = material(
            pending,
            "PROOF_ARCHIVE",
            "proof.zip",
            "private/proof.zip"
        );
        CertificationMaterial detailVideo = material(
            pending,
            "DETAIL_VIDEO",
            "detail.mp4",
            "private/detail.mp4"
        );
        pending.getMaterials().addAll(List.of(proof, detailVideo));
        when(certifications.findByUserIdOrderByIdAsc(7L)).thenReturn(List.of(pending));
        when(storage.accessUrl("private/detail.mp4", StorageVisibility.PRIVATE))
            .thenReturn("https://signed.example.com/detail.mp4");

        CertificationService.CertificationView result = service.list(user).get(0);

        assertEquals("", result.materials().get(0).url());
        assertEquals("https://signed.example.com/detail.mp4", result.materials().get(1).url());
        verify(storage, never()).accessUrl("private/proof.zip", StorageVisibility.PRIVATE);
    }

    @Test
    void pendingExperienceCannotBeChanged() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            mock(FileStorage.class),
            mock(SensitiveWordService.class),
            mock(FileTypeDetector.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(7L);
        Certification pending = certification(user, "EXPERIENCE");
        pending.setId(11L);
        pending.setCategory("EXPERIENCE");
        pending.setStatus("PENDING");
        when(users.findWithLockById(7L)).thenReturn(Optional.of(user));
        when(certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            7L,
            "IDENTITY",
            "APPROVED"
        )).thenReturn(true);
        when(certifications.findByIdAndUserId(11L, 7L)).thenReturn(Optional.of(pending));

        com.shixianwen.common.BusinessException error = assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitExperience(
                user,
                11L,
                "修改后的标题",
                "修改后的详述",
                true,
                "TEXT",
                null,
                null,
                null,
                null,
                List.of()
            )
        );

        assertEquals("只有已驳回的经历才能修改", error.getMessage());
        verify(certifications, never()).save(any(Certification.class));
    }

    @Test
    void experienceApprovalDoesNotEnableInquiriesBeforePriceIsConfigured() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            mock(FileStorage.class),
            mock(SensitiveWordService.class),
            mock(FileTypeDetector.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(7L);
        user.setAcceptingInquiries(false);
        Certification identity = certification(user, "IDENTITY");
        Certification experience = certification(user, "EXPERIENCE");
        experience.setCategory("EXPERIENCE");
        experience.setId(2L);
        experience.setStatus("PENDING");
        when(certifications.findById(2L)).thenReturn(Optional.of(experience));
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(certifications.findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(7L, "APPROVED"))
            .thenReturn(List.of(identity, experience));

        service.review(2L, true, null);

        assertEquals("APPROVED", user.getAnswererStatus());
        assertFalse(user.isAcceptingInquiries());
        verify(users).save(user);
    }

    @Test
    void identityApprovalAloneAllowsSubmittingExperience() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        FileStorage storage = mock(FileStorage.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        FileTypeDetector fileTypeDetector = mock(FileTypeDetector.class);
        UserRepository users = mock(UserRepository.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            storage,
            sensitiveWords,
            fileTypeDetector,
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(7L);
        user.setUid("7996702");
        when(users.findWithLockById(7L)).thenReturn(Optional.of(user));
        MockMultipartFile archive = new MockMultipartFile(
            "files",
            "proof.zip",
            "application/zip",
            new byte[]{1}
        );
        MockMultipartFile originalArchive = new MockMultipartFile(
            "reviewOriginal",
            "original.zip",
            "application/zip",
            new byte[]{1}
        );
        MockMultipartFile signature = new MockMultipartFile(
            "signature",
            "signature.png",
            "image/png",
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
        when(fileTypeDetector.detect(any(org.springframework.web.multipart.MultipartFile.class)))
            .thenReturn(new FileTypeDetector.DetectedFile("ARCHIVE", "application/zip", ".zip"));
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
            true,
            "TEXT",
            signature,
            originalArchive,
            archive,
            null,
            List.of()
        );

        assertEquals("经历标题", result.title());
        assertEquals(3, result.materials().size());
        assertEquals("REVIEW_ORIGINAL_ARCHIVE", result.materials().get(0).kind());
        assertEquals("PROOF_ARCHIVE", result.materials().get(1).kind());
        assertEquals("SIGNATURE", result.materials().get(2).kind());
    }

    @Test
    void writtenAndVideoDetailsCanBeSubmittedTogether() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        FileStorage storage = mock(FileStorage.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        FileTypeDetector fileTypeDetector = mock(FileTypeDetector.class);
        UserRepository users = mock(UserRepository.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            storage,
            sensitiveWords,
            fileTypeDetector,
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(8L);
        user.setUid("7996703");
        MockMultipartFile detailVideo = new MockMultipartFile(
            "detailVideo",
            "detail.mp4",
            "video/mp4",
            new byte[]{1}
        );
        MockMultipartFile proofArchive = new MockMultipartFile(
            "files",
            "proof.zip",
            "application/zip",
            new byte[]{1}
        );
        MockMultipartFile originalArchive = new MockMultipartFile(
            "reviewOriginal",
            "original.zip",
            "application/zip",
            new byte[]{1}
        );
        MockMultipartFile signature = new MockMultipartFile(
            "signature",
            "signature.png",
            "image/png",
            new byte[]{1}
        );
        when(users.findWithLockById(8L)).thenReturn(Optional.of(user));
        when(certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            8L,
            "IDENTITY",
            "APPROVED"
        )).thenReturn(true);
        when(sensitiveWords.mask(any(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(fileTypeDetector.detect(any(org.springframework.web.multipart.MultipartFile.class)))
            .thenAnswer(invocation -> {
                org.springframework.web.multipart.MultipartFile file = invocation.getArgument(0);
                if (file.getOriginalFilename().endsWith(".mp4")) {
                    return new FileTypeDetector.DetectedFile("VIDEO", "video/mp4", ".mp4");
                }
                return new FileTypeDetector.DetectedFile("ARCHIVE", "application/zip", ".zip");
            });
        when(storage.store(any(), any(String.class), any(StorageVisibility.class)))
            .thenAnswer(invocation -> {
                org.springframework.web.multipart.MultipartFile file = invocation.getArgument(0);
                String name = file.getOriginalFilename();
                return new StoredFile("certifications/" + name, null, file.getContentType(), 1);
            });
        when(storage.accessUrl(any(String.class), any(StorageVisibility.class)))
            .thenAnswer(invocation -> "https://signed.example.com/" + invocation.getArgument(0));
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CertificationService.CertificationView result = service.submitExperience(
            user,
            null,
            "经历标题",
            "文字详述",
            true,
            "BOTH",
            signature,
            originalArchive,
            proofArchive,
            detailVideo,
            List.of()
        );

        assertEquals("文字详述", result.description());
        assertEquals("DETAIL_VIDEO", result.materials().get(0).kind());
        assertEquals("REVIEW_ORIGINAL_ARCHIVE", result.materials().get(1).kind());
        assertEquals("PROOF_ARCHIVE", result.materials().get(2).kind());
        assertEquals("SIGNATURE", result.materials().get(3).kind());
    }

    @Test
    void publicWelfareExperienceDoesNotRequireIdentityOrOriginalArchive() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        FileStorage storage = mock(FileStorage.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            storage,
            sensitiveWords,
            mock(FileTypeDetector.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(9L);
        user.setUid("7996704");
        MockMultipartFile proofArchive = new MockMultipartFile(
            "proofArchive", "shared.zip", "application/zip", new byte[]{1}
        );
        when(users.findWithLockById(9L)).thenReturn(Optional.of(user));
        when(sensitiveWords.mask(any(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.store(any(), any(String.class), any(StorageVisibility.class)))
            .thenReturn(new StoredFile("certifications/shared.zip", null, "application/zip", 1));
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CertificationService.CertificationView result = service.submitPublicWelfareExperience(
            user, null, "实用内容", "把事情讲清楚", "TEXT", proofArchive, null
        );

        assertEquals("PUBLIC_WELFARE", result.experienceBusinessType());
        assertEquals(1, result.materials().size());
        assertEquals("PROOF_ARCHIVE", result.materials().get(0).kind());
        verify(certifications, never()).existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(
            9L, "IDENTITY", "APPROVED"
        );
    }

    @Test
    void monetizedExperienceRequiresIdentityBeforeAcceptingSubmission() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            mock(FileStorage.class),
            mock(SensitiveWordService.class),
            mock(FileTypeDetector.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(10L);
        when(users.findWithLockById(10L)).thenReturn(Optional.of(user));

        com.shixianwen.common.BusinessException error = assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitMonetizedExperience(
                user, null, null, "经历标题", "经历详述", "TEXT",
                true, null, null, null, null
            )
        );

        assertEquals("完成实名认证后才能发布干货变现经历", error.getMessage());
        verify(certifications, never()).save(any(Certification.class));
    }

    private Certification certification(User user, String type) {
        Certification certification = new Certification();
        certification.setUser(user);
        certification.setCertificationType(type);
        certification.setStatus("APPROVED");
        certification.setEnabled(true);
        return certification;
    }

    private CertificationMaterial material(
        Certification certification,
        String kind,
        String name,
        String storageKey
    ) {
        CertificationMaterial material = new CertificationMaterial();
        material.setCertification(certification);
        material.setMaterialKind(kind);
        material.setOriginalName(name);
        material.setStorageKey(storageKey);
        material.setContentType("application/octet-stream");
        return material;
    }
}

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CertificationServiceTest {
    @Test
    void experienceAdditionalInformationIsStoredWithTheExperience() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            mock(FileStorage.class),
            sensitiveWords,
            mock(FileTypeDetector.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(18L);
        user.setUid("7996718");
        when(users.findWithLockById(18L)).thenReturn(Optional.of(user));
        when(sensitiveWords.mask(any(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CertificationService.CertificationView result = service.submitExperience(
            user,
            null,
            "我的装修经历",
            "2024年，我作为房主经历过房屋装修。",
            "北京市朝阳区",
            "2024年1月1日",
            "2024年6月1日",
            1,
            "房主",
            "24～33岁",
            "本科",
            "销售",
            false,
            null,
            null,
            List.of()
        );

        assertEquals("北京市朝阳区", result.experienceLocation());
        assertEquals("2024年1月1日", result.experienceStartDate());
        assertEquals("2024年6月1日", result.experienceEndDate());
        assertEquals(1, result.experienceCount());
        assertEquals("房主", result.experienceRole());
        assertEquals("24～33岁", result.experienceAgeRange());
        assertEquals("本科", result.experienceEducation());
        assertEquals("销售", result.experienceJob());
    }

    @Test
    void experienceAdditionalInformationUsesPublishedLengthLimits() {
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
        user.setId(19L);
        user.setUid("7996719");
        when(users.findWithLockById(19L)).thenReturn(Optional.of(user));

        assertEquals("发生地点最多20个字", assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitExperience(
                user, null, "经历标题", "经历叙述", "地".repeat(21), null, null,
                null, null, null, null, null, false, null, null, List.of()
            )
        ).getMessage());
        assertEquals("开始时间最多20个字", assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitExperience(
                user, null, "经历标题", "经历叙述", null, "时".repeat(21), null,
                null, null, null, null, null, false, null, null, List.of()
            )
        ).getMessage());
        assertEquals("结束时间最多20个字", assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitExperience(
                user, null, "经历标题", "经历叙述", null, null, "时".repeat(21),
                null, null, null, null, null, false, null, null, List.of()
            )
        ).getMessage());
        assertEquals("已经历的次数只能填写1至99", assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitExperience(
                user, null, "经历标题", "经历叙述", null, null, null,
                100, null, null, null, null, false, null, null, List.of()
            )
        ).getMessage());
        assertEquals("本人当时的身份最多7个字", assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitExperience(
                user, null, "经历标题", "经历叙述", null, null, null,
                null, "身份".repeat(4), null, null, null, false, null, null, List.of()
            )
        ).getMessage());
        assertEquals("当时职业最多12个字", assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitExperience(
                user, null, "经历标题", "经历叙述", null, null, null,
                null, null, null, null, "职业".repeat(7), false, null, null, List.of()
            )
        ).getMessage());
    }

    @Test
    void rejectedExperienceCanBeLogicallyDeleted() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        com.shixianwen.analytics.AnalyticsEventService analytics =
            mock(com.shixianwen.analytics.AnalyticsEventService.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            mock(FileStorage.class),
            mock(SensitiveWordService.class),
            mock(FileTypeDetector.class),
            analytics
        );
        User user = new User();
        user.setId(7L);
        Certification pending = certification(user, "EXPERIENCE");
        pending.setId(11L);
        pending.setCategory("EXPERIENCE");
        pending.setStatus("REJECTED");
        CertificationMaterial material = material(
            pending,
            "PROOF_ARCHIVE",
            "proof.zip",
            "private/proof.zip"
        );
        pending.getMaterials().add(material);
        when(certifications.findByIdAndUserId(11L, 7L)).thenReturn(Optional.of(pending));

        service.deleteExperience(user, 11L);

        assertFalse(pending.isEnabled());
        assertNotNull(pending.getDeletedAt());
        assertNotNull(material.getDeletedAt());
        verify(certifications).save(pending);
    }

    @Test
    void experienceUnderReviewCannotBeDeleted() {
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
        Certification approved = certification(user, "EXPERIENCE");
        approved.setId(12L);
        approved.setCategory("EXPERIENCE");
        approved.setStatus("PENDING");
        when(certifications.findByIdAndUserId(12L, 7L)).thenReturn(Optional.of(approved));

        com.shixianwen.common.BusinessException error = assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.deleteExperience(user, 12L)
        );

        assertEquals("只有已被驳回的经历才能删除", error.getMessage());
        verify(certifications, never()).save(any(Certification.class));
    }

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
        CertificationMaterial legacyOriginal = material(
            pending,
            "REVIEW_ORIGINAL_ARCHIVE",
            "original.zip",
            "private/original.zip"
        );
        CertificationMaterial detailVideo = material(
            pending,
            "DETAIL_VIDEO",
            "detail.mp4",
            "private/detail.mp4"
        );
        pending.getMaterials().addAll(List.of(legacyOriginal, proof, detailVideo));
        when(certifications.findByUserIdOrderByIdAsc(7L)).thenReturn(List.of(pending));
        CertificationService.CertificationView result = service.list(user).get(0);

        assertEquals("", result.materials().get(0).url());
        assertEquals(1, result.materials().size());
        verify(storage, never()).accessUrl("private/original.zip", StorageVisibility.PRIVATE);
        verify(storage, never()).accessUrl("private/proof.zip", StorageVisibility.PRIVATE);
        verify(storage, never()).accessUrl("private/detail.mp4", StorageVisibility.PRIVATE);
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
        when(certifications.findByIdAndUserId(11L, 7L)).thenReturn(Optional.of(pending));

        com.shixianwen.common.BusinessException error = assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitExperience(
                user,
                11L,
                "修改后的标题",
                "修改后的详述",
                false,
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
        Certification experience = certification(user, "EXPERIENCE");
        experience.setCategory("EXPERIENCE");
        experience.setId(2L);
        experience.setStatus("PENDING");
        when(certifications.findById(2L)).thenReturn(Optional.of(experience));
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(certifications.findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(7L, "APPROVED"))
            .thenReturn(List.of(experience));

        service.review(2L, true, null);

        assertEquals("APPROVED", user.getAnswererStatus());
        assertFalse(user.isAcceptingInquiries());
        verify(users).save(user);
    }

    @Test
    void experienceWithoutProofCanBeApprovedAndSkipsMediaProcessing() {
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
        user.setId(8L);
        Certification experience = certification(user, "EXPERIENCE");
        experience.setCategory("EXPERIENCE");
        experience.setId(3L);
        experience.setStatus("PENDING");
        when(certifications.findById(3L)).thenReturn(Optional.of(experience));
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(certifications.findByUserIdAndStatusAndEnabledTrueOrderByIdAsc(8L, "APPROVED"))
            .thenReturn(List.of(experience));

        CertificationService.CertificationView result = service.review(3L, true, null);

        assertEquals("APPROVED", result.status());
        assertEquals("NOT_REQUIRED", result.mediaProcessingStatus());
        verify(certifications).save(experience);
    }

    @Test
    void experienceCanIncludeAnOptionalProofArchive() {
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
            false,
            null,
            archive,
            List.of()
        );

        assertEquals("经历标题", result.title());
        assertEquals(1, result.materials().size());
        assertEquals("PROOF_ARCHIVE", result.materials().get(0).kind());
    }

    @Test
    void proofMaterialsAreOptionalAsAWhole() {
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
        when(users.findWithLockById(8L)).thenReturn(Optional.of(user));
        when(sensitiveWords.mask(any(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.store(any(), any(String.class), any(StorageVisibility.class)))
            .thenReturn(new StoredFile("certifications/signature.png", null, "image/png", 1));
        when(storage.accessUrl(any(String.class), any(StorageVisibility.class)))
            .thenAnswer(invocation -> "https://signed.example.com/" + invocation.getArgument(0));
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CertificationService.CertificationView result = service.submitExperience(
            user,
            null,
            "经历标题",
            "文字详述",
            false,
            null,
            null,
            List.of()
        );

        assertEquals("文字详述", result.description());
        assertEquals(0, result.materials().size());
    }

    @Test
    void rejectedExperienceCanRemoveAnExistingProofArchive() {
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
        user.setId(12L);
        user.setUid("7996712");
        Certification rejected = certification(user, "EXPERIENCE");
        rejected.setId(21L);
        rejected.setCategory("EXPERIENCE");
        rejected.setStatus("REJECTED");
        CertificationMaterial existingProof = material(
            rejected,
            "PROOF_ARCHIVE",
            "old-proof.zip",
            "private/old-proof.zip"
        );
        rejected.getMaterials().add(existingProof);
        when(users.findWithLockById(12L)).thenReturn(Optional.of(user));
        when(certifications.findByIdAndUserId(21L, 12L)).thenReturn(Optional.of(rejected));
        when(sensitiveWords.mask(any(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.store(any(), any(String.class), any(StorageVisibility.class)))
            .thenReturn(new StoredFile("certifications/signature.png", null, "image/png", 1));
        when(storage.accessUrl(any(String.class), any(StorageVisibility.class)))
            .thenAnswer(invocation -> "https://signed.example.com/" + invocation.getArgument(0));
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CertificationService.CertificationView result = service.submitExperience(
            user,
            21L,
            "经历标题",
            "文字详述",
            true,
            null,
            null,
            List.of()
        );

        assertNotNull(existingProof.getDeletedAt());
        assertEquals(0, result.materials().size());
    }

    @Test
    void legacyFilesParameterCanSupplyTheSingleOptionalProofArchive() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        FileTypeDetector fileTypeDetector = mock(FileTypeDetector.class);
        FileStorage storage = mock(FileStorage.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            storage,
            sensitiveWords,
            fileTypeDetector,
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
        when(storage.accessUrl(any(String.class), any(StorageVisibility.class)))
            .thenReturn("https://signed.example.com/shared.zip");
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CertificationService.CertificationView result = service.submitExperience(
            user, null, "实用内容", "把事情讲清楚", false,
            null, null, List.of(proofArchive)
        );

        assertEquals("PROOF_ARCHIVE", result.materials().get(0).kind());
    }

    @Test
    void writtenDescriptionIsRequiredAndLimitedToFourHundredCharacters() {
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
        user.setUid("7996710");
        when(users.findWithLockById(10L)).thenReturn(Optional.of(user));

        com.shixianwen.common.BusinessException missing = assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitExperience(
                user, null, "实用内容", "  ", false, null, null, List.of()
            )
        );
        com.shixianwen.common.BusinessException tooLong = assertThrows(
            com.shixianwen.common.BusinessException.class,
            () -> service.submitExperience(
                user, null, "实用内容", "经".repeat(401), false, null, null, List.of()
            )
        );

        assertEquals("请填写文字详述", missing.getMessage());
        assertEquals("文字详述最多400个字", tooLong.getMessage());
    }

    @Test
    void experienceCanBeSubmittedWithoutAnyPriorCertification() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        SensitiveWordService sensitiveWords = mock(SensitiveWordService.class);
        CertificationService service = new CertificationService(
            certifications,
            users,
            mock(FileStorage.class),
            sensitiveWords,
            mock(FileTypeDetector.class),
            mock(com.shixianwen.analytics.AnalyticsEventService.class)
        );
        User user = new User();
        user.setId(10L);
        user.setUid("7996710");
        when(users.findWithLockById(10L)).thenReturn(Optional.of(user));
        when(sensitiveWords.mask(any(String.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(certifications.save(any(Certification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CertificationService.CertificationView result = service.submitExperience(
            user, null, "经历标题", "经历详述",
            false, null, null, List.of()
        );

        assertEquals("经历标题", result.title());
        verify(certifications).save(any(Certification.class));
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

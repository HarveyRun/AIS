package com.shixianwen.admin;

import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.certification.JobCertificationAppointment;
import com.shixianwen.certification.JobCertificationAppointmentRepository;
import com.shixianwen.common.BusinessException;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.FileTypeDetector;
import com.shixianwen.storage.StoredFile;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminJobCertificationAppointmentServiceTest {
    @Test
    void approvalRequiresAndPersistsAudioEvidenceThenReusesOnlineReview() {
        JobCertificationAppointmentRepository appointments =
            mock(JobCertificationAppointmentRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        AdminManagementService management = mock(AdminManagementService.class);
        FileStorage storage = mock(FileStorage.class);
        NotificationService notifications = mock(NotificationService.class);
        AdminAuditLogRepository audits = mock(AdminAuditLogRepository.class);
        FileTypeDetector detector = new FileTypeDetector();
        User user = user();
        JobCertificationAppointment appointment = appointment(user);
        AdminUser admin = admin();
        MockMultipartFile audio = new MockMultipartFile(
            "evidence",
            "现场录音.mp3",
            "audio/mpeg",
            new byte[] {'I', 'D', '3', 4, 0, 0, 0, 0, 0, 0}
        );

        when(appointments.findWithLockById(12L)).thenReturn(Optional.of(appointment));
        when(storage.store(any(), any(), eq(StorageVisibility.PRIVATE)))
            .thenReturn(new StoredFile("private/certifications/evidence.mp3", null, "audio/mpeg", audio.getSize()));
        when(certifications.saveAndFlush(any(Certification.class))).thenAnswer(invocation -> {
            Certification certification = invocation.getArgument(0);
            certification.setId(31L);
            return certification;
        });

        service(appointments, certifications, management, storage, detector, notifications, audits)
            .process(admin, 12L, "APPROVED", "", 9L, 8, 100, audio, "127.0.0.1");

        assertThat(appointment.getStatus()).isEqualTo("APPROVED");
        assertThat(appointment.getCertification()).isNotNull();
        assertThat(appointment.getCertification().getMaterials()).hasSize(1);
        assertThat(appointment.getCertification().getMaterials().get(0).getMaterialKind())
            .isEqualTo("AUDIO");
        verify(management).reviewCertification(
            admin, 31L, true, null, 9L, 8, null, 100, "127.0.0.1"
        );
        verify(appointments).save(appointment);
        verify(notifications).send(
            user,
            "岗位认证已通过",
            "您的线下岗位认证已经通过，可前往基础认证查看结果。",
            "/profile/certifications/basic"
        );
    }

    @Test
    void approvalWithoutEvidenceIsRejected() {
        JobCertificationAppointmentRepository appointments =
            mock(JobCertificationAppointmentRepository.class);
        JobCertificationAppointment appointment = appointment(user());
        when(appointments.findWithLockById(12L)).thenReturn(Optional.of(appointment));

        AdminJobCertificationAppointmentService service = service(
            appointments,
            mock(CertificationRepository.class),
            mock(AdminManagementService.class),
            mock(FileStorage.class),
            new FileTypeDetector(),
            mock(NotificationService.class),
            mock(AdminAuditLogRepository.class)
        );

        assertThatThrownBy(
            () -> service.process(admin(), 12L, "APPROVED", "", 9L, 8, 100, null, "127.0.0.1")
        )
            .isInstanceOf(BusinessException.class)
            .hasMessage("认证通过前必须上传现场录音或录像");
    }

    private AdminJobCertificationAppointmentService service(
        JobCertificationAppointmentRepository appointments,
        CertificationRepository certifications,
        AdminManagementService management,
        FileStorage storage,
        FileTypeDetector detector,
        NotificationService notifications,
        AdminAuditLogRepository audits
    ) {
        return new AdminJobCertificationAppointmentService(
            mock(JdbcTemplate.class),
            appointments,
            certifications,
            management,
            storage,
            detector,
            notifications,
            audits
        );
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setUid("7996702");
        user.setAccountType("NORMAL");
        return user;
    }

    private JobCertificationAppointment appointment(User user) {
        JobCertificationAppointment appointment = new JobCertificationAppointment();
        appointment.setId(12L);
        appointment.setUser(user);
        appointment.setStatus("BOOKED");
        appointment.setAppointmentAt(LocalDateTime.now().minusHours(1));
        return appointment;
    }

    private AdminUser admin() {
        AdminUser admin = new AdminUser();
        admin.setId(3L);
        return admin;
    }
}

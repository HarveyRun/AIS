package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobCertificationAppointmentServiceTest {
    @Test
    void weekendAppointmentIsRecordedWithoutSubmittingCertification() {
        JobCertificationAppointmentRepository appointments =
            mock(JobCertificationAppointmentRepository.class);
        UserRepository users = mock(UserRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        User user = new User();
        user.setId(7L);
        LocalDateTime saturday = LocalDateTime.now()
            .with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
            .withHour(11)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

        when(users.findWithLockById(7L)).thenReturn(Optional.of(user));
        when(certifications.findFirstByUserIdAndCertificationTypeOrderByIdDesc(7L, "MAIN_JOB"))
            .thenReturn(Optional.empty());
        when(appointments
            .findFirstByUserIdAndStatusOrderByAppointmentAtDesc(
                any(), any()
            ))
            .thenReturn(Optional.empty());
        when(appointments.saveAndFlush(any(JobCertificationAppointment.class)))
            .thenAnswer(invocation -> {
                JobCertificationAppointment item = invocation.getArgument(0);
                item.setId(12L);
                return item;
            });

        JobCertificationAppointmentService.AppointmentView result =
            new JobCertificationAppointmentService(appointments, users, certifications)
                .book(user, saturday);

        assertEquals(12L, result.id());
        assertEquals("北京", result.city());
        assertEquals("BOOKED", result.status());
        verify(certifications, never()).save(any());
    }

    @Test
    void weekdayAppointmentIsRejected() {
        JobCertificationAppointmentRepository appointments =
            mock(JobCertificationAppointmentRepository.class);
        LocalDateTime monday = LocalDateTime.now()
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .withHour(11)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);
        User user = new User();
        user.setId(7L);
        JobCertificationAppointmentService service =
            new JobCertificationAppointmentService(
                appointments,
                mock(UserRepository.class),
                mock(CertificationRepository.class)
            );

        BusinessException error = assertThrows(
            BusinessException.class,
            () -> service.book(user, monday)
        );

        assertEquals("线下认证仅可预约周六或周日", error.getMessage());
        verify(appointments, never()).saveAndFlush(any());
    }

    @Test
    void occupiedSlotIsReportedUnavailable() {
        JobCertificationAppointmentRepository appointments =
            mock(JobCertificationAppointmentRepository.class);
        LocalDateTime saturday = LocalDateTime.now()
            .with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
            .withHour(15)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);
        when(appointments.existsByAppointmentAtAndStatus(saturday, "BOOKED")).thenReturn(true);

        JobCertificationAppointmentService.AvailabilityView result =
            new JobCertificationAppointmentService(
                appointments,
                mock(UserRepository.class),
                mock(CertificationRepository.class)
            ).availability(saturday);

        assertEquals(false, result.available());
    }
}

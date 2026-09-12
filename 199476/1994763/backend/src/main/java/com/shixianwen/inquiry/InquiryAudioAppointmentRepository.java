package com.shixianwen.inquiry;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InquiryAudioAppointmentRepository
    extends JpaRepository<InquiryAudioAppointment, Long> {

    @EntityGraph(attributePaths = {"inquiry", "questioner", "answerer"})
    Optional<InquiryAudioAppointment> findTopByInquiryIdOrderByIdDesc(Long inquiryId);

    @EntityGraph(attributePaths = {"inquiry", "inquiry.questioner", "inquiry.answerer", "questioner", "answerer"})
    @Query("select appointment from InquiryAudioAppointment appointment where appointment.id=:id")
    Optional<InquiryAudioAppointment> findDetailById(@Param("id") Long id);

    long countByInquiryIdAndAppointmentType(Long inquiryId, String appointmentType);

    boolean existsByInquiryIdAndStatusIn(Long inquiryId, Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"inquiry", "inquiry.questioner", "inquiry.answerer", "questioner", "answerer"})
    @Query("select appointment from InquiryAudioAppointment appointment where appointment.id=:id")
    Optional<InquiryAudioAppointment> findWithLockById(@Param("id") Long id);

    @Query("""
        select count(appointment) > 0 from InquiryAudioAppointment appointment
        where appointment.questioner.id=:userId
          and appointment.id<>:appointmentId
          and appointment.status in ('PENDING','ACCEPTED','CONNECTING','ACTIVE')
          and appointment.scheduledStartAt<:endAt
          and appointment.scheduledEndAt>:startAt
        """)
    boolean hasQuestionerOverlap(
        @Param("userId") Long userId,
        @Param("appointmentId") Long appointmentId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );

    @Query("""
        select count(appointment) > 0 from InquiryAudioAppointment appointment
        where appointment.answerer.id=:userId
          and appointment.id<>:appointmentId
          and appointment.status in ('PENDING','ACCEPTED','CONNECTING','ACTIVE')
          and appointment.scheduledStartAt<:endAt
          and appointment.scheduledEndAt>:startAt
        """)
    boolean hasAnswererOverlap(
        @Param("userId") Long userId,
        @Param("appointmentId") Long appointmentId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );

    @Query("""
        select count(appointment) > 0 from InquiryAudioAppointment appointment
        where (appointment.questioner.id=:userId or appointment.answerer.id=:userId)
          and appointment.id<>:appointmentId
          and appointment.status in ('PENDING','ACCEPTED','CONNECTING','ACTIVE')
          and appointment.scheduledStartAt<:endAt
          and appointment.scheduledEndAt>:startAt
        """)
    boolean hasParticipantOverlap(
        @Param("userId") Long userId,
        @Param("appointmentId") Long appointmentId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );

    List<InquiryAudioAppointment> findTop100ByStatusAndResponseDeadlineBeforeOrderByResponseDeadlineAsc(
        String status,
        LocalDateTime now
    );

    List<InquiryAudioAppointment> findTop100ByStatusAndScheduledStartAtBeforeOrderByScheduledStartAtAsc(
        String status,
        LocalDateTime now
    );

    List<InquiryAudioAppointment> findTop100ByStatusAndScheduledEndAtBeforeOrderByScheduledEndAtAsc(
        String status,
        LocalDateTime now
    );

    List<InquiryAudioAppointment> findTop100ByStatusAndConnectDeadlineBeforeOrderByConnectDeadlineAsc(
        String status,
        LocalDateTime now
    );

    List<InquiryAudioAppointment> findTop100ByStatusAndReconnectDeadlineBeforeOrderByReconnectDeadlineAsc(
        String status,
        LocalDateTime now
    );

    List<InquiryAudioAppointment> findTop100ByStatusAndFiveMinuteWarningSentFalseAndScheduledEndAtBetweenOrderByScheduledEndAtAsc(
        String status,
        LocalDateTime from,
        LocalDateTime to
    );
}

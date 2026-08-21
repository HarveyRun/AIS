package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JobCertificationAppointmentService {
    private static final String BOOKED = "BOOKED";
    private static final String CITY = "北京";

    private final JobCertificationAppointmentRepository appointments;
    private final UserRepository users;
    private final CertificationRepository certifications;

    @Transactional(readOnly = true)
    public AppointmentView current(User user) {
        return appointments
            .findFirstByUserIdAndStatusOrderByAppointmentAtDesc(
                user.getId(),
                BOOKED
            )
            .map(AppointmentView::from)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public AvailabilityView availability(LocalDateTime appointmentAt) {
        LocalDateTime slot = validateSlot(appointmentAt);
        return new AvailabilityView(
            slot,
            !appointments.existsByAppointmentAtAndStatus(slot, BOOKED)
        );
    }

    @Transactional
    public AppointmentView book(User currentUser, LocalDateTime appointmentAt) {
        LocalDateTime slot = validateSlot(appointmentAt);
        User user = users.findWithLockById(currentUser.getId())
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        JobCertificationAuthenticityPolicy.requireCanApply(user, LocalDateTime.now());
        Certification existingCertification = certifications
            .findFirstByUserIdAndCertificationTypeOrderByIdDesc(user.getId(), "MAIN_JOB")
            .orElse(null);
        if (existingCertification != null &&
            !"REJECTED".equals(existingCertification.getStatus())) {
            throw BusinessException.badRequest("岗位认证已经提交，无需重复预约");
        }
        boolean alreadyBooked = appointments
            .findFirstByUserIdAndStatusOrderByAppointmentAtDesc(
                user.getId(),
                BOOKED
            )
            .isPresent();
        if (alreadyBooked) {
            throw BusinessException.badRequest("您已有待进行的线下认证预约");
        }
        if (appointments.existsByAppointmentAtAndStatus(slot, BOOKED)) {
            throw BusinessException.badRequest("该时间已被预约，请选择其他时间");
        }

        JobCertificationAppointment appointment = new JobCertificationAppointment();
        appointment.setUser(user);
        appointment.setAppointmentAt(slot);
        appointment.setCity(CITY);
        appointment.setStatus(BOOKED);
        try {
            return AppointmentView.from(appointments.saveAndFlush(appointment));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.badRequest("该时间刚刚被预约，请选择其他时间");
        }
    }

    private LocalDateTime validateSlot(LocalDateTime appointmentAt) {
        if (appointmentAt == null) {
            throw BusinessException.badRequest("请选择预约时间");
        }
        LocalDateTime slot = appointmentAt.withSecond(0).withNano(0);
        if (!slot.isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("预约时间必须晚于当前时间");
        }
        DayOfWeek day = slot.getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
            throw BusinessException.badRequest("线下认证仅可预约周六或周日");
        }
        if (slot.getMinute() != 0 || (slot.getHour() != 11 && slot.getHour() != 15)) {
            throw BusinessException.badRequest("线下认证仅可预约上午11:00或下午15:00");
        }
        return slot;
    }

    public record AvailabilityView(LocalDateTime appointmentAt, boolean available) {
    }

    public record AppointmentView(
        Long id,
        LocalDateTime appointmentAt,
        String city,
        String status
    ) {
        static AppointmentView from(JobCertificationAppointment appointment) {
            return new AppointmentView(
                appointment.getId(),
                appointment.getAppointmentAt(),
                appointment.getCity(),
                appointment.getStatus()
            );
        }
    }
}

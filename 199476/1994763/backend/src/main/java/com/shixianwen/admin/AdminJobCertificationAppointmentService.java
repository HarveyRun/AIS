package com.shixianwen.admin;

import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationMaterial;
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
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminJobCertificationAppointmentService {
    private static final String BOOKED = "BOOKED";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String CANCELLED = "CANCELLED";
    private static final String NO_SHOW = "NO_SHOW";
    private static final long MAX_EVIDENCE_SIZE = 500L * 1024 * 1024;

    private final JdbcTemplate jdbc;
    private final JobCertificationAppointmentRepository appointments;
    private final CertificationRepository certifications;
    private final AdminManagementService adminManagementService;
    private final FileStorage fileStorage;
    private final FileTypeDetector fileTypeDetector;
    private final NotificationService notifications;
    private final AdminAuditLogRepository audits;

    @Transactional(readOnly = true)
    public PageResult list(String keyword, String status, int page, int size) {
        String query = keyword == null ? "" : keyword.trim();
        String normalizedStatus = normalizeListStatus(status);
        String like = "%" + query + "%";
        String where = " WHERE (?='' OR u.uid LIKE ? OR u.phone LIKE ? "
            + "OR COALESCE(u.nickname,'') LIKE ?) AND (?='' OR a.status=?) ";
        Object[] parameters = {query, like, like, like, normalizedStatus, normalizedStatus};
        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM job_certification_appointments a "
                + "JOIN users u ON u.id=a.user_id" + where,
            Long.class,
            parameters
        );
        List<Map<String, Object>> items = jdbc.queryForList(
            "SELECT a.id,u.uid,u.phone,u.nickname,a.appointment_at AS appointmentAt,"
                + "a.city,a.status,a.result_reason AS resultReason,"
                + "a.certification_id AS certificationId,"
                + "certification.authenticity_percent AS authenticityPercent,"
                + "certification.job_reapply_available_at AS jobReapplyAvailableAt,"
                + "administrator.display_name AS processedBy,a.processed_at AS processedAt,"
                + "a.created_at AS createdAt "
                + "FROM job_certification_appointments a "
                + "JOIN users u ON u.id=a.user_id "
                + "LEFT JOIN certifications certification ON certification.id=a.certification_id "
                + "LEFT JOIN admin_users administrator ON administrator.id=a.processed_by_admin_id"
                + where
                + "ORDER BY CASE WHEN a.status='BOOKED' THEN 0 ELSE 1 END,"
                + "a.appointment_at ASC,a.id DESC LIMIT ? OFFSET ?",
            append(parameters, size, page * size)
        );
        return new PageResult(items, total == null ? 0 : total, page, size);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> materials(Long appointmentId) {
        List<Long> certificationIds = jdbc.query(
            "SELECT certification_id FROM job_certification_appointments "
                + "WHERE id=? AND certification_id IS NOT NULL",
            (resultSet, rowNumber) -> resultSet.getLong(1),
            appointmentId
        );
        if (certificationIds.isEmpty()) return List.of();
        return jdbc.queryForList(
            "SELECT id,material_kind AS kind,original_name AS name,"
                + "storage_key AS storageKey,public_url AS publicUrl,"
                + "content_type AS contentType,file_size AS size "
                + "FROM certification_materials "
                + "WHERE certification_id=? AND deleted_at IS NULL ORDER BY id",
            certificationIds.get(0)
        ).stream().map(this::withAccessUrl).toList();
    }

    @Transactional
    public void process(
        AdminUser admin,
        Long appointmentId,
        String status,
        String reason,
        Long jobId,
        Integer years,
        Integer authenticityPercent,
        MultipartFile evidence,
        String ipAddress
    ) {
        String targetStatus = normalizeProcessStatus(status);
        JobCertificationAppointment appointment = appointments.findWithLockById(appointmentId)
            .orElseThrow(() -> BusinessException.notFound("线下认证预约不存在"));
        if (!BOOKED.equals(appointment.getStatus())) {
            throw BusinessException.badRequest("该线下认证预约已经处理");
        }
        if ((APPROVED.equals(targetStatus) || REJECTED.equals(targetStatus))
            && appointment.getAppointmentAt().isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("预约时间尚未到，不能确认认证结果");
        }

        String normalizedReason = reason == null ? "" : reason.trim();
        if ((REJECTED.equals(targetStatus) || CANCELLED.equals(targetStatus))
            && normalizedReason.isBlank()) {
            throw BusinessException.badRequest(
                REJECTED.equals(targetStatus) ? "认证不通过时请填写原因" : "取消预约时请填写原因"
            );
        }

        Certification certification = null;
        if (APPROVED.equals(targetStatus)) {
            validateApproval(jobId, years, evidence);
            certification = createCertification(appointment.getUser(), evidence);
            adminManagementService.reviewCertification(
                admin,
                certification.getId(),
                true,
                null,
                jobId,
                years,
                null,
                authenticityPercent,
                ipAddress
            );
        } else if (REJECTED.equals(targetStatus)) {
            certification = createCertification(appointment.getUser(), null);
            adminManagementService.reviewCertification(
                admin,
                certification.getId(),
                false,
                normalizedReason,
                null,
                null,
                null,
                authenticityPercent,
                ipAddress
            );
        }

        appointment.setStatus(targetStatus);
        appointment.setCertification(certification);
        appointment.setResultReason(normalizedReason.isBlank() ? null : normalizedReason);
        appointment.setProcessedByAdmin(admin);
        appointment.setProcessedAt(LocalDateTime.now());
        appointments.save(appointment);

        sendResultNotification(appointment);
        audit(
            admin,
            "PROCESS_OFFLINE_JOB_CERTIFICATION",
            appointmentId,
            targetStatus + (normalizedReason.isBlank() ? "" : ":" + normalizedReason),
            ipAddress
        );
    }

    private void validateApproval(Long jobId, Integer years, MultipartFile evidence) {
        if (jobId == null) throw BusinessException.badRequest("请选择认证岗位");
        if (years == null || years < 5 || years > 80) {
            throw BusinessException.badRequest("工龄必须是5至80之间的整数");
        }
        if (evidence == null || evidence.isEmpty()) {
            throw BusinessException.badRequest("认证通过前必须上传现场录音或录像");
        }
        if (evidence.getSize() > MAX_EVIDENCE_SIZE) {
            throw BusinessException.badRequest("录音或录像不能超过500MB");
        }
        fileTypeDetector.requireAudioOrVideo(evidence);
    }

    private Certification createCertification(User user, MultipartFile evidence) {
        Certification certification = new Certification();
        certification.setUser(user);
        certification.setCategory("BASIC");
        certification.setCertificationType("MAIN_JOB");
        certification.setTitle("");
        certification.setDescription("线下岗位认证");
        certification.setRequiredItem(true);
        certification.setStatus("PENDING");
        certification.setSubmittedAt(LocalDateTime.now());

        if (evidence != null && !evidence.isEmpty()) {
            FileTypeDetector.DetectedFile detected = fileTypeDetector.requireAudioOrVideo(evidence);
            StoredFile stored = fileStorage.store(
                evidence,
                storagePrefix(user) + "certifications/" + user.getUid(),
                StorageVisibility.PRIVATE
            );
            CertificationMaterial material = new CertificationMaterial();
            material.setCertification(certification);
            material.setMaterialKind(detected.kind());
            material.setOriginalName(
                evidence.getOriginalFilename() == null
                    ? ("AUDIO".equals(detected.kind()) ? "线下认证录音" : "线下认证录像")
                    : evidence.getOriginalFilename()
            );
            material.setStorageKey(stored.storageKey());
            material.setPublicUrl(null);
            material.setContentType(stored.contentType());
            material.setFileSize(stored.size());
            certification.getMaterials().add(material);
        }
        return certifications.saveAndFlush(certification);
    }

    private void sendResultNotification(JobCertificationAppointment appointment) {
        String status = appointment.getStatus();
        String title;
        String content;
        if (APPROVED.equals(status)) {
            title = "岗位认证已通过";
            content = "您的线下岗位认证已经通过，可前往基础认证查看结果。";
        } else if (REJECTED.equals(status)) {
            title = "岗位认证未通过";
            content = "您的线下岗位认证未通过：" + appointment.getResultReason();
        } else if (NO_SHOW.equals(status)) {
            title = "线下认证预约已结束";
            content = "本次预约记录为未到场，如仍需认证可以重新预约。";
        } else {
            title = "线下认证预约已取消";
            content = "本次预约已取消：" + appointment.getResultReason();
        }
        notifications.send(
            appointment.getUser(),
            title,
            content,
            "/profile/certifications/basic"
        );
    }

    private Map<String, Object> withAccessUrl(Map<String, Object> source) {
        Map<String, Object> item = new LinkedHashMap<>(source);
        String publicUrl = Objects.toString(item.remove("publicUrl"), "");
        String storageKey = Objects.toString(item.remove("storageKey"), "");
        item.put(
            "url",
            publicUrl.isBlank()
                ? fileStorage.accessUrl(storageKey, StorageVisibility.PRIVATE)
                : publicUrl
        );
        return item;
    }

    private String normalizeListStatus(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!List.of(BOOKED, APPROVED, REJECTED, CANCELLED, NO_SHOW).contains(normalized)) {
            throw BusinessException.badRequest("预约状态不正确");
        }
        return normalized;
    }

    private String normalizeProcessStatus(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of(APPROVED, REJECTED, CANCELLED, NO_SHOW).contains(normalized)) {
            throw BusinessException.badRequest("请选择正确的处理结果");
        }
        return normalized;
    }

    private String storagePrefix(User user) {
        return "TEST".equals(user.getAccountType()) ? "test/" : "";
    }

    private void audit(
        AdminUser admin,
        String action,
        Object id,
        String detail,
        String ipAddress
    ) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUser(admin);
        log.setAction(action);
        log.setTargetType("JOB_CERTIFICATION_APPOINTMENT");
        log.setTargetId(String.valueOf(id));
        log.setDetail(detail);
        log.setIpAddress(ipAddress);
        audits.save(log);
    }

    private Object[] append(Object[] source, Object... values) {
        List<Object> result = new ArrayList<>(List.of(source));
        result.addAll(List.of(values));
        return result.toArray();
    }

    public record PageResult(List<Map<String, Object>> items, long total, int page, int size) {
    }
}

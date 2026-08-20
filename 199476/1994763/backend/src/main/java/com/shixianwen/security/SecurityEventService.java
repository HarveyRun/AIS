package com.shixianwen.security;

import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityEventService {
    private final SecurityEventRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
        Long userId,
        Long adminUserId,
        String eventType,
        String severity,
        String requestIp,
        String deviceId,
        String detail
    ) {
        SecurityEvent event = new SecurityEvent();
        event.setUserId(userId);
        event.setAdminUserId(adminUserId);
        event.setEventType(limit(eventType, 60));
        event.setSeverity(normalizeSeverity(severity));
        event.setRequestIp(limit(requestIp, 45));
        event.setDeviceId(limit(deviceId, 100));
        event.setDetail(limit(detail, 1000));
        repository.save(event);
        if ("HIGH".equals(event.getSeverity()) || "CRITICAL".equals(event.getSeverity())) {
            log.warn(
                "SECURITY_EVENT type={} severity={} userId={} adminId={} ip={} detail={}",
                event.getEventType(), event.getSeverity(), userId, adminUserId,
                event.getRequestIp(), event.getDetail()
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSafely(
        Long userId,
        Long adminUserId,
        String eventType,
        String severity,
        String requestIp,
        String deviceId,
        String detail
    ) {
        try {
            SecurityEvent event = new SecurityEvent();
            event.setUserId(userId);
            event.setAdminUserId(adminUserId);
            event.setEventType(limit(eventType, 60));
            event.setSeverity(normalizeSeverity(severity));
            event.setRequestIp(limit(requestIp, 45));
            event.setDeviceId(limit(deviceId, 100));
            event.setDetail(limit(detail, 1000));
            repository.save(event);
            warnIfImportant(event);
        } catch (RuntimeException exception) {
            log.error("Unable to persist security event type={}", eventType, exception);
        }
    }

    @Transactional(readOnly = true)
    public PageView list(String severity, String status, String type, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Specification<SecurityEvent> spec = (root, query, cb) -> cb.conjunction();
        if (severity != null && !severity.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity.trim().toUpperCase()));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reviewStatus"), status.trim().toUpperCase()));
        }
        if (type != null && !type.isBlank()) {
            String keyword = "%" + type.trim().toUpperCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.upper(root.get("eventType")), keyword));
        }
        Page<SecurityEvent> result = repository.findAll(
            spec,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return new PageView(result.getContent().stream().map(View::from).toList(), result.getTotalElements(), safePage, safeSize);
    }

    @Transactional
    public View review(AdminUser admin, Long id) {
        SecurityEvent event = repository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("安全事件不存在"));
        event.setReviewStatus("REVIEWED");
        event.setReviewedByAdminId(admin.getId());
        event.setReviewedAt(LocalDateTime.now());
        return View.from(event);
    }

    private static String normalizeSeverity(String severity) {
        String value = severity == null ? "INFO" : severity.trim().toUpperCase();
        return switch (value) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> value;
            default -> "INFO";
        };
    }

    private void warnIfImportant(SecurityEvent event) {
        if ("HIGH".equals(event.getSeverity()) || "CRITICAL".equals(event.getSeverity())) {
            log.warn(
                "SECURITY_EVENT type={} severity={} userId={} adminId={} ip={} detail={}",
                event.getEventType(), event.getSeverity(), event.getUserId(), event.getAdminUserId(),
                event.getRequestIp(), event.getDetail()
            );
        }
    }

    private static String limit(String value, int max) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    public record View(
        Long id,
        Long userId,
        Long adminUserId,
        String eventType,
        String severity,
        String requestIp,
        String deviceId,
        String detail,
        String reviewStatus,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
    ) {
        static View from(SecurityEvent event) {
            return new View(
                event.getId(), event.getUserId(), event.getAdminUserId(), event.getEventType(),
                event.getSeverity(), event.getRequestIp(), event.getDeviceId(), event.getDetail(),
                event.getReviewStatus(), event.getReviewedByAdminId(), event.getReviewedAt(), event.getCreatedAt()
            );
        }
    }

    public record PageView(List<View> items, long total, int page, int size) {
    }
}

package com.shixianwen.certification;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CertificationMediaRecovery {
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;

    public CertificationMediaRecovery(JdbcTemplate jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumePendingWork() {
        jdbc.update(
            "UPDATE certifications SET media_processing_status='PENDING' " +
                "WHERE category='EXPERIENCE' AND status='APPROVED' " +
                "AND media_processing_status='PROCESSING' AND deleted_at IS NULL"
        );
        jdbc.queryForList(
            "SELECT id FROM certifications WHERE category='EXPERIENCE' " +
                "AND status='APPROVED' AND media_processing_status IN ('PENDING','PROCESSING') " +
                "AND deleted_at IS NULL ORDER BY id",
            Long.class
        ).forEach(id -> events.publishEvent(new CertificationMediaExtractionRequested(id)));
    }
}

package com.shixianwen.notification;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@RequiredArgsConstructor
public class SystemAnnouncementScheduler {
    private final SystemAnnouncementRepository repository;
    private final ObjectProvider<SystemAnnouncementService> serviceProvider;
    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    @PostConstruct
    void initialize() {
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("announcement-publisher-");
        scheduler.initialize();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restoreSchedules() {
        repository
            .findAllByStatusAndDeletedAtIsNull("SCHEDULED")
            .forEach(item -> schedule(item.getId(), item.getScheduledAt()));
    }

    public void schedule(Long id, LocalDateTime scheduledAt) {
        cancel(id);
        if (id == null || scheduledAt == null) return;
        ScheduledFuture<?> task = scheduler.schedule(
            () -> {
                tasks.remove(id);
                serviceProvider.getObject().publishScheduled(id, scheduledAt);
            },
            scheduledAt.atZone(ZoneId.systemDefault()).toInstant()
        );
        if (task != null) tasks.put(id, task);
    }

    public void cancel(Long id) {
        ScheduledFuture<?> task = tasks.remove(id);
        if (task != null) task.cancel(false);
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdown();
    }
}

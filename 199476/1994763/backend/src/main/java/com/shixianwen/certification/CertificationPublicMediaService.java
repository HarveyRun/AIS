package com.shixianwen.certification;

import com.shixianwen.common.BusinessException;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CertificationPublicMediaService {
    private final CertificationRepository certifications;
    private final CertificationPublicMediaRepository mediaRepository;
    private final FileStorage storage;
    private final ApplicationEventPublisher events;

    public CertificationPublicMediaService(
        CertificationRepository certifications,
        CertificationPublicMediaRepository mediaRepository,
        FileStorage storage,
        ApplicationEventPublisher events
    ) {
        this.certifications = certifications;
        this.mediaRepository = mediaRepository;
        this.storage = storage;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public PublicMediaView list(User user, Long certificationId) {
        Certification certification = ownedApprovedExperience(user, certificationId);
        return view(certification);
    }

    @Transactional
    public PublicMediaView update(User user, Long certificationId, List<Long> selectedIds) {
        Certification certification = ownedApprovedExperience(user, certificationId);
        if (!"READY".equals(certification.getMediaProcessingStatus())) {
            throw BusinessException.badRequest("证明资料尚未整理完成");
        }
        Set<Long> requested = new HashSet<>(selectedIds == null ? List.of() : selectedIds);
        List<CertificationPublicMedia> all = mediaRepository
            .findByCertificationIdOrderBySortOrderAscIdAsc(certificationId);
        Set<Long> available = all.stream().map(CertificationPublicMedia::getId)
            .collect(java.util.stream.Collectors.toSet());
        if (!available.containsAll(requested)) {
            throw BusinessException.badRequest("选择的公开内容不存在");
        }
        all.forEach(item -> item.setPublicSelected(requested.contains(item.getId())));
        mediaRepository.saveAll(all);
        return view(certification);
    }

    @Transactional
    public void retry(Long certificationId) {
        Certification certification = certifications.findById(certificationId)
            .filter(item -> "EXPERIENCE".equals(item.getCategory()))
            .filter(item -> "APPROVED".equals(item.getStatus()))
            .orElseThrow(() -> BusinessException.notFound("已通过的经历不存在"));
        if ("PROCESSING".equals(certification.getMediaProcessingStatus())) {
            throw BusinessException.badRequest("证明资料正在整理中");
        }
        certification.setMediaProcessingStatus("PENDING");
        certification.setMediaProcessingError(null);
        certification.setMediaProcessedAt(null);
        certifications.save(certification);
        events.publishEvent(new CertificationMediaExtractionRequested(certificationId));
    }

    @Transactional(readOnly = true)
    public List<PublicMediaItem> publicItems(Long certificationId) {
        return mediaRepository
            .findByCertificationIdAndPublicSelectedTrueOrderBySortOrderAscIdAsc(certificationId)
            .stream()
            .map(this::itemView)
            .toList();
    }

    private Certification ownedApprovedExperience(User user, Long certificationId) {
        return certifications.findById(certificationId)
            .filter(item -> item.getUser().getId().equals(user.getId()))
            .filter(item -> "EXPERIENCE".equals(item.getCategory()))
            .filter(item -> "APPROVED".equals(item.getStatus()))
            .orElseThrow(() -> BusinessException.notFound("已通过的经历不存在"));
    }

    private PublicMediaView view(Certification certification) {
        List<PublicMediaItem> items = mediaRepository
            .findByCertificationIdOrderBySortOrderAscIdAsc(certification.getId())
            .stream()
            .map(this::itemView)
            .toList();
        return new PublicMediaView(
            certification.getId(),
            certification.getMediaProcessingStatus(),
            certification.getMediaProcessingError(),
            items
        );
    }

    private PublicMediaItem itemView(CertificationPublicMedia item) {
        return new PublicMediaItem(
            item.getId(),
            item.getMediaType(),
            item.getDisplayName(),
            storage.accessUrl(item.getStorageKey(), StorageVisibility.PRIVATE),
            item.getFileSize(),
            item.getContentType(),
            item.isPublicSelected()
        );
    }

    public record PublicMediaView(
        Long certificationId,
        String processingStatus,
        String processingError,
        List<PublicMediaItem> items
    ) {
    }

    public record PublicMediaItem(
        Long id,
        String kind,
        String name,
        String url,
        long size,
        String contentType,
        boolean selected
    ) {
    }
}

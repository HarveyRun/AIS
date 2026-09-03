package com.shixianwen.certification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CertificationPublicMediaRepository extends JpaRepository<CertificationPublicMedia, Long> {
    List<CertificationPublicMedia> findByCertificationIdOrderBySortOrderAscIdAsc(Long certificationId);

    List<CertificationPublicMedia> findByCertificationIdAndPublicSelectedTrueOrderBySortOrderAscIdAsc(
        Long certificationId
    );

    List<CertificationPublicMedia> findByCertificationIdAndIdIn(
        Long certificationId,
        Collection<Long> ids
    );
}

package com.shixianwen.version;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
    Page<AppVersion> findAllByDeletedFalse(Pageable pageable);

    Optional<AppVersion> findByIdAndDeletedFalse(Long id);

    Optional<AppVersion> findFirstByPlatformAndPublishedTrueAndDeletedFalseOrderByVersionCodeDesc(
        String platform
    );

    boolean existsByPlatformAndVersionCodeAndDeletedFalse(
        String platform,
        int versionCode
    );

    boolean existsByPlatformAndVersionCodeAndDeletedFalseAndIdNot(
        String platform,
        int versionCode,
        Long id
    );

    @Modifying
    @Query("""
        update AppVersion version
           set version.published = false,
               version.publishedAt = null
         where version.platform = :platform
           and version.id <> :id
           and version.published = true
           and version.deleted = false
        """)
    void unpublishOthers(@Param("platform") String platform, @Param("id") Long id);
}

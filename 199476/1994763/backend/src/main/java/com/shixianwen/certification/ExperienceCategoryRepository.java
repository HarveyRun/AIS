package com.shixianwen.certification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperienceCategoryRepository extends JpaRepository<ExperienceCategory, Long> {
    List<ExperienceCategory> findByDeletedAtIsNullOrderBySortOrderAscIdAsc();
}

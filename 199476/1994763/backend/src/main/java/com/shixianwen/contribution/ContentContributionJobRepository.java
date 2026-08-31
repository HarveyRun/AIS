package com.shixianwen.contribution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentContributionJobRepository extends JpaRepository<ContentContributionJob, Long> {
    List<ContentContributionJob> findByContributionIdOrderBySortOrderAscIdAsc(Long contributionId);
}

package com.shixianwen.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCommunicationBlockRepository
    extends JpaRepository<UserCommunicationBlock, Long> {

    boolean existsByUserLowIdAndUserHighId(Long userLowId, Long userHighId);

    Optional<UserCommunicationBlock> findByUserLowIdAndUserHighId(
        Long userLowId,
        Long userHighId
    );
}

package com.shixianwen.invitation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.Optional;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, Long> {
    Optional<UserInvitation> findByInviteeId(Long inviteeId);
    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from UserInvitation item where item.id = :id")
    Optional<UserInvitation> findWithLockById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"inviter", "invitee", "reviewedByAdmin"})
    @Query("""
        select item from UserInvitation item
        where (:keyword = ''
            or item.inviter.uid like concat('%', :keyword, '%')
            or item.invitee.uid like concat('%', :keyword, '%')
            or item.inviter.phone like concat('%', :keyword, '%')
            or item.invitee.phone like concat('%', :keyword, '%')
            or item.inviterRealName like concat('%', :keyword, '%'))
          and (:status = '' or item.status = :status)
        """)
    Page<UserInvitation> search(
        @Param("keyword") String keyword,
        @Param("status") String status,
        Pageable pageable
    );

    @Query("select coalesce(sum(item.rewardAmount), 0) from UserInvitation item where item.status = :status")
    BigDecimal sumRewardAmountByStatus(@Param("status") String status);
}

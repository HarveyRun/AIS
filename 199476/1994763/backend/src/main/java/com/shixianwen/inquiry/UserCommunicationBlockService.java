package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserCommunicationBlockService {
    private static final Set<String> ENDED_INQUIRY_STATUSES = Set.of("COMPLETED", "ENDED");

    private final UserCommunicationBlockRepository blocks;
    private final InquiryRepository inquiries;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public boolean isBlocked(Long firstUserId, Long secondUserId) {
        UserPair pair = pair(firstUserId, secondUserId);
        return blocks.existsByUserLowIdAndUserHighId(pair.low(), pair.high());
    }

    @Transactional(readOnly = true)
    public void requireCommunicationAllowed(Long firstUserId, Long secondUserId) {
        if (isBlocked(firstUserId, secondUserId)) {
            throw BusinessException.forbidden("你与对方已无法继续交流");
        }
    }

    @Transactional
    public BlockView block(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiries.findWithLockById(inquiryId)
            .orElseThrow(() -> BusinessException.notFound("询问不存在"));
        Long otherUserId = otherParticipantId(inquiry, userId);
        requireEnded(inquiry);
        UserPair pair = pair(userId, otherUserId);
        User low = lockedUser(pair.low());
        User high = lockedUser(pair.high());
        return blocks.findByUserLowIdAndUserHighId(pair.low(), pair.high())
            .map(BlockView::of)
            .orElseGet(() -> {
                UserCommunicationBlock block = new UserCommunicationBlock();
                block.setUserLow(low);
                block.setUserHigh(high);
                block.setBlockedByUser(userId.equals(low.getId()) ? low : high);
                block.setSourceInquiry(inquiry);
                return BlockView.of(blocks.saveAndFlush(block));
            });
    }

    private Long otherParticipantId(Inquiry inquiry, Long userId) {
        if (inquiry.getQuestioner().getId().equals(userId)) {
            return inquiry.getAnswerer().getId();
        }
        if (inquiry.getAnswerer().getId().equals(userId)) {
            return inquiry.getQuestioner().getId();
        }
        throw BusinessException.forbidden("无权操作该询问");
    }

    private void requireEnded(Inquiry inquiry) {
        String status = inquiry.getStatus();
        if (status == null || !ENDED_INQUIRY_STATUSES.contains(status)) {
            throw BusinessException.badRequest("询问结束后才能拉黑");
        }
    }

    private User lockedUser(Long id) {
        return users.findWithLockById(id)
            .orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    private UserPair pair(Long firstUserId, Long secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) {
            throw BusinessException.badRequest("无效的用户关系");
        }
        return firstUserId < secondUserId
            ? new UserPair(firstUserId, secondUserId)
            : new UserPair(secondUserId, firstUserId);
    }

    private record UserPair(Long low, Long high) {}

    public record BlockView(boolean blocked, Long blockedByUserId, LocalDateTime createdAt) {
        static BlockView of(UserCommunicationBlock block) {
            return new BlockView(
                true,
                block.getBlockedByUser().getId(),
                block.getCreatedAt()
            );
        }
    }
}

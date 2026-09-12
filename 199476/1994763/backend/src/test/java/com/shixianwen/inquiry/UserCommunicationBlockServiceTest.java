package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserCommunicationBlockServiceTest {
    @Test
    void participantCanCreateOneCanonicalBidirectionalBlock() {
        UserCommunicationBlockRepository blocks = mock(UserCommunicationBlockRepository.class);
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserRepository users = mock(UserRepository.class);
        UserCommunicationBlockService service = new UserCommunicationBlockService(
            blocks,
            inquiries,
            users
        );
        User questioner = user(9L);
        User answerer = user(2L);
        Inquiry inquiry = inquiry(31L, questioner, answerer);
        inquiry.setStatus("COMPLETED");
        when(inquiries.findWithLockById(31L)).thenReturn(Optional.of(inquiry));
        when(users.findWithLockById(2L)).thenReturn(Optional.of(answerer));
        when(users.findWithLockById(9L)).thenReturn(Optional.of(questioner));
        when(blocks.findByUserLowIdAndUserHighId(2L, 9L)).thenReturn(Optional.empty());
        when(blocks.saveAndFlush(any(UserCommunicationBlock.class))).thenAnswer(invocation -> {
            UserCommunicationBlock saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        UserCommunicationBlockService.BlockView result = service.block(9L, 31L);

        assertTrue(result.blocked());
        assertEquals(9L, result.blockedByUserId());
        verify(blocks).saveAndFlush(any(UserCommunicationBlock.class));
    }

    @Test
    void activeInquiryCannotBeUsedToBlockTheOtherParticipant() {
        UserCommunicationBlockRepository blocks = mock(UserCommunicationBlockRepository.class);
        InquiryRepository inquiries = mock(InquiryRepository.class);
        UserCommunicationBlockService service = new UserCommunicationBlockService(
            blocks,
            inquiries,
            mock(UserRepository.class)
        );
        Inquiry inquiry = inquiry(31L, user(9L), user(2L));
        inquiry.setStatus("ACTIVE");
        when(inquiries.findWithLockById(31L)).thenReturn(Optional.of(inquiry));

        assertThrows(BusinessException.class, () -> service.block(9L, 31L));
    }

    @Test
    void blockPreventsCommunicationInEitherDirection() {
        UserCommunicationBlockRepository blocks = mock(UserCommunicationBlockRepository.class);
        UserCommunicationBlockService service = new UserCommunicationBlockService(
            blocks,
            mock(InquiryRepository.class),
            mock(UserRepository.class)
        );
        when(blocks.existsByUserLowIdAndUserHighId(2L, 9L)).thenReturn(true);

        assertThrows(
            BusinessException.class,
            () -> service.requireCommunicationAllowed(9L, 2L)
        );
        assertThrows(
            BusinessException.class,
            () -> service.requireCommunicationAllowed(2L, 9L)
        );
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Inquiry inquiry(Long id, User questioner, User answerer) {
        Inquiry inquiry = new Inquiry();
        inquiry.setId(id);
        inquiry.setQuestioner(questioner);
        inquiry.setAnswerer(answerer);
        return inquiry;
    }
}

package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private static final Set<String> OPEN = Set.of("PENDING", "ACTIVE", "AWAITING_CONFIRMATION", "DISPUTED");
    private final InquiryRepository inquiries;
    private final InquiryMessageRepository messages;
    private final UserRepository users;
    private final WalletService wallet;
    private final NotificationService notifications;

    @Transactional
    public InquiryView create(Long questionerId, CreateCommand command) {
        if (questionerId.equals(command.answererId())) throw BusinessException.badRequest("不能向自己发起询问");
        User questioner = user(questionerId);
        User answerer = user(command.answererId());
        if (!"APPROVED".equals(answerer.getAnswererStatus())) throw BusinessException.badRequest("对方尚未成为答主");
        if (!answerer.isAcceptingInquiries()) throw BusinessException.badRequest("对方暂不接受询问");
        if (inquiries.existsByQuestionerIdAndAnswererIdAndStatusIn(questionerId, answerer.getId(), OPEN))
            throw BusinessException.badRequest("你们已有一条进行中的询问");
        Inquiry item = new Inquiry();
        item.setQuestioner(questioner); item.setAnswerer(answerer); item.setTopic(clean(command.topic(), 120));
        item.setSourceType(command.sourceType() == null ? "PROFILE" : command.sourceType());
        item.setQuestion(required(command.question(), "请填写想问的事情", 1000));
        item.setAmount(command.amount()); item.setStatus("PENDING"); item.setFundsStatus("FROZEN");
        item.setResponseDeadline(LocalDateTime.now().plusHours(24));
        item = inquiries.save(item);
        wallet.freeze(questionerId, item.getAmount(), item.getId());
        notifications.send(answerer, "收到新的询问", "有人想向你问一件事", "/inquiries/" + item.getId());
        return view(item, questionerId);
    }

    public List<InquiryView> list(Long userId) {
        return inquiries.findByQuestionerIdOrAnswererIdOrderByCreatedAtDesc(userId, userId).stream()
                .map(i -> view(i, userId)).toList();
    }

    public InquiryDetail detail(Long userId, Long inquiryId) {
        Inquiry item = accessible(userId, inquiryId);
        return new InquiryDetail(view(item, userId), messages.findByInquiryIdOrderByCreatedAtAsc(inquiryId).stream()
                .map(MessageView::of).toList());
    }

    @Transactional
    public InquiryView accept(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, true);
        requireStatus(item, "PENDING");
        if (!item.getAnswerer().isAcceptingInquiries()) throw BusinessException.badRequest("你已暂停接受询问");
        item.setStatus("ACTIVE"); item.setAcceptedAt(LocalDateTime.now()); item.setResponseDeadline(null);
        notifications.send(item.getQuestioner(), "对方已接受询问", "现在可以开始交流了", "/inquiries/" + item.getId());
        return view(item, userId);
    }

    @Transactional
    public InquiryView reject(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, true);
        requireStatus(item, "PENDING");
        refund(item, "REJECTED");
        notifications.send(item.getQuestioner(), "询问未被接受", "冻结金额已退回余额", "/inquiries/" + item.getId());
        return view(item, userId);
    }

    @Transactional
    public InquiryView cancel(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, false);
        requireStatus(item, "PENDING");
        refund(item, "CANCELLED");
        notifications.send(item.getAnswerer(), "询问已撤销", "对方撤销了这次询问", "/inquiries/" + item.getId());
        return view(item, userId);
    }

    @Transactional
    public MessageView send(Long userId, Long inquiryId, String content) {
        Inquiry item = accessible(userId, inquiryId);
        requireStatus(item, "ACTIVE");
        InquiryMessage message = new InquiryMessage();
        message.setInquiry(item); message.setSender(user(userId));
        message.setContent(required(content, "消息不能为空", 2000));
        return MessageView.of(messages.save(message));
    }

    @Transactional
    public InquiryView requestEnd(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, true);
        requireStatus(item, "ACTIVE");
        item.setStatus("AWAITING_CONFIRMATION"); item.setConfirmationDeadline(LocalDateTime.now().plusHours(48));
        notifications.send(item.getQuestioner(), "对方申请结束交流", "请确认结束或继续交流", "/inquiries/" + item.getId());
        return view(item, userId);
    }

    @Transactional
    public InquiryView continueChat(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, false);
        requireStatus(item, "AWAITING_CONFIRMATION");
        item.setStatus("ACTIVE"); item.setConfirmationDeadline(null);
        return view(item, userId);
    }

    @Transactional
    public InquiryView confirmEnd(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, false);
        if (!Set.of("ACTIVE", "AWAITING_CONFIRMATION").contains(item.getStatus()))
            throw BusinessException.badRequest("当前状态不能结束交流");
        settle(item);
        return view(item, userId);
    }

    @Scheduled(fixedDelayString = "${app.inquiry.timeout-scan-ms:60000}")
    @Transactional
    public void processTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        inquiries.findByStatusAndResponseDeadlineBefore("PENDING", now).forEach(i -> {
            Inquiry item = inquiries.findWithLockById(i.getId()).orElse(null);
            if (item != null && "PENDING".equals(item.getStatus())) refund(item, "EXPIRED");
        });
        inquiries.findByStatusAndConfirmationDeadlineBefore("AWAITING_CONFIRMATION", now).forEach(i -> {
            Inquiry item = inquiries.findWithLockById(i.getId()).orElse(null);
            if (item != null && "AWAITING_CONFIRMATION".equals(item.getStatus())) settle(item);
        });
    }

    private void refund(Inquiry item, String status) {
        wallet.refund(item.getQuestioner().getId(), item.getAmount(), item.getId());
        item.setStatus(status); item.setFundsStatus("REFUNDED"); item.setResponseDeadline(null);
    }
    private void settle(Inquiry item) {
        wallet.settle(item.getQuestioner().getId(), item.getAnswerer().getId(), item.getAmount(), item.getId());
        item.setStatus("COMPLETED"); item.setFundsStatus("SETTLED"); item.setEndedAt(LocalDateTime.now());
        item.setConfirmationDeadline(null);
        notifications.send(item.getAnswerer(), "交流已结束", "费用已进入账户余额", "/inquiries/" + item.getId());
    }
    private Inquiry lockedParticipant(Long userId, Long id, boolean answerer) {
        Inquiry item = inquiries.findWithLockById(id).orElseThrow(() -> BusinessException.notFound("询问不存在"));
        Long owner = answerer ? item.getAnswerer().getId() : item.getQuestioner().getId();
        if (!owner.equals(userId)) throw BusinessException.forbidden("无权执行该操作");
        return item;
    }
    private Inquiry accessible(Long userId, Long id) {
        Inquiry item = inquiries.findById(id).orElseThrow(() -> BusinessException.notFound("询问不存在"));
        if (!item.getQuestioner().getId().equals(userId) && !item.getAnswerer().getId().equals(userId))
            throw BusinessException.forbidden("无权查看该询问");
        return item;
    }
    private User user(Long id) { return users.findById(id).orElseThrow(() -> BusinessException.notFound("用户不存在")); }
    private void requireStatus(Inquiry i, String status) { if (!status.equals(i.getStatus())) throw BusinessException.badRequest("当前状态不能执行该操作"); }
    private String required(String s, String message, int max) { if (s == null || s.isBlank()) throw BusinessException.badRequest(message); return clean(s, max); }
    private String clean(String s, int max) { if (s == null) return null; String v = s.trim(); return v.length() <= max ? v : v.substring(0, max); }

    private InquiryView view(Inquiry i, Long me) {
        User other = i.getQuestioner().getId().equals(me) ? i.getAnswerer() : i.getQuestioner();
        return new InquiryView(i.getId(), i.getQuestioner().getId().equals(me) ? "QUESTIONER" : "ANSWERER",
                other.getId(), other.getNickname() == null || other.getNickname().isBlank() ? other.getUid() : other.getNickname(),
                other.getAvatarUrl(), i.getTopic(), i.getQuestion(), i.getAmount(), i.getStatus(), i.getFundsStatus(),
                i.getResponseDeadline(), i.getConfirmationDeadline(), i.getCreatedAt());
    }
    public record CreateCommand(Long answererId, String topic, String sourceType, String question, BigDecimal amount) {}
    public record InquiryView(Long id, String role, Long otherUserId, String otherName, String otherAvatar, String topic,
                              String question, BigDecimal amount, String status, String fundsStatus,
                              LocalDateTime responseDeadline, LocalDateTime confirmationDeadline, LocalDateTime createdAt) {}
    public record MessageView(Long id, Long senderId, String senderName, String senderAvatar, String type, String content,
                              String attachmentUrl, LocalDateTime createdAt) {
        static MessageView of(InquiryMessage m) { User s = m.getSender(); return new MessageView(m.getId(), s.getId(), s.getNickname() == null || s.getNickname().isBlank() ? s.getUid() : s.getNickname(), s.getAvatarUrl(), m.getMessageType(), m.getContent(), m.getAttachmentUrl(), m.getCreatedAt()); }
    }
    public record InquiryDetail(InquiryView inquiry, List<MessageView> messages) {}
}

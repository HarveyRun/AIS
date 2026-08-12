package com.shixianwen.inquiry;

import com.shixianwen.common.BusinessException;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.WalletService;
import com.shixianwen.network.ClientNetworkInfo;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
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
    private final RealtimePublisher realtime;
    private final FileStorage fileStorage;

    @Transactional
    public InquiryView create(Long questionerId, CreateCommand command, ClientNetworkInfo network) {
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
        item.setRequestIp(network.ipAddress());
        item.setRequestLocation(network.location());
        item.setAmount(command.amount()); item.setStatus("PENDING"); item.setFundsStatus("FROZEN");
        item.setAnswererUnreadCount(1);
        item.setResponseDeadline(LocalDateTime.now().plusHours(24));
        item = inquiries.save(item);
        wallet.freeze(questionerId, item.getAmount(), item.getId());
        notifications.send(answerer, "收到新的询问", displayName(questioner) + "：" + notificationSubject(item), "/inquiries/" + item.getId());
        publishInquiryChanged(answerer.getId(), item);
        return view(item, questionerId);
    }

    @Transactional(readOnly = true)
    public List<InquiryView> list(Long userId) {
        return inquiries.findByQuestionerIdOrAnswererIdOrderByCreatedAtDesc(userId, userId).stream()
                .map(i -> view(i, userId)).toList();
    }

    @Transactional(readOnly = true)
    public InquiryDetail detail(Long userId, Long inquiryId) {
        Inquiry item = accessible(userId, inquiryId);
        return new InquiryDetail(view(item, userId), messages.findByInquiryIdOrderByCreatedAtAsc(inquiryId).stream()
                .map(MessageView::of).toList());
    }

    @Transactional
    public void read(Long userId, Long inquiryId) {
        Inquiry item = lockedAccessible(userId, inquiryId);
        if (item.getQuestioner().getId().equals(userId)) item.setQuestionerUnreadCount(0);
        else item.setAnswererUnreadCount(0);
        realtime.afterCommit(userId, "INQUIRY_READ", java.util.Map.of("inquiryId", inquiryId));
    }

    @Transactional
    public InquiryView accept(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, true);
        requireStatus(item, "PENDING");
        if (!item.getAnswerer().isAcceptingInquiries()) throw BusinessException.badRequest("你已暂停接受询问");
        item.setAnswererUnreadCount(0);
        item.setStatus("ACTIVE"); item.setAcceptedAt(LocalDateTime.now()); item.setResponseDeadline(null);
        increaseUnread(item, item.getQuestioner().getId());
        notifications.send(item.getQuestioner(), "询问已接受", displayName(item.getAnswerer()) + "已接受：" + notificationSubject(item), "/inquiries/" + item.getId());
        publishInquiryChanged(item.getQuestioner().getId(), item);
        return view(item, userId);
    }

    @Transactional
    public InquiryView reject(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, true);
        requireStatus(item, "PENDING");
        item.setAnswererUnreadCount(0);
        refund(item, "REJECTED");
        increaseUnread(item, item.getQuestioner().getId());
        notifications.send(item.getQuestioner(), "询问未被接受", notificationSubject(item) + "；冻结金额已退回余额", "/inquiries/" + item.getId());
        publishInquiryChanged(item.getQuestioner().getId(), item);
        return view(item, userId);
    }

    @Transactional
    public InquiryView cancel(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, false);
        requireStatus(item, "PENDING");
        item.setQuestionerUnreadCount(0);
        refund(item, "CANCELLED");
        increaseUnread(item, item.getAnswerer().getId());
        notifications.send(item.getAnswerer(), "询问已撤销", displayName(item.getQuestioner()) + "撤销了：" + notificationSubject(item), "/inquiries/" + item.getId());
        publishInquiryChanged(item.getAnswerer().getId(), item);
        return view(item, userId);
    }

    @Transactional
    public MessageView send(Long userId, Long inquiryId, String content) {
        Inquiry item = lockedAccessible(userId, inquiryId);
        requireStatus(item, "ACTIVE");
        if (item.getQuestioner().getId().equals(userId)) item.setQuestionerUnreadCount(0);
        else item.setAnswererUnreadCount(0);
        InquiryMessage message = new InquiryMessage();
        message.setInquiry(item); message.setSender(user(userId));
        message.setContent(required(content, "消息不能为空", 2000));
        message = messages.saveAndFlush(message);
        item.setLastMessageAt(message.getCreatedAt());
        Long recipientId = item.getQuestioner().getId().equals(userId)
            ? item.getAnswerer().getId()
            : item.getQuestioner().getId();
        increaseUnread(item, recipientId);
        MessageView view = MessageView.of(message);
        realtime.afterCommit(recipientId, "INQUIRY_MESSAGE", new MessageEvent(
            inquiryId, unreadFor(item, recipientId), view
        ));
        return view;
    }

    @Transactional
    public MessageView sendImage(Long userId, Long inquiryId, MultipartFile image) {
        Inquiry item = lockedAccessible(userId, inquiryId);
        requireStatus(item, "ACTIVE");
        validateChatImage(image);
        if (item.getQuestioner().getId().equals(userId)) item.setQuestionerUnreadCount(0);
        else item.setAnswererUnreadCount(0);

        StoredFile stored = fileStorage.store(image, "inquiries/" + inquiryId);
        InquiryMessage message = new InquiryMessage();
        message.setInquiry(item);
        message.setSender(user(userId));
        message.setMessageType("IMAGE");
        message.setContent("");
        message.setAttachmentUrl(stored.publicUrl());
        message.setAttachmentName(clean(image.getOriginalFilename(), 255));
        message.setAttachmentSize(stored.size());
        message = messages.saveAndFlush(message);
        item.setLastMessageAt(message.getCreatedAt());

        Long recipientId = item.getQuestioner().getId().equals(userId)
            ? item.getAnswerer().getId()
            : item.getQuestioner().getId();
        increaseUnread(item, recipientId);
        MessageView view = MessageView.of(message);
        realtime.afterCommit(recipientId, "INQUIRY_MESSAGE", new MessageEvent(
            inquiryId, unreadFor(item, recipientId), view
        ));
        return view;
    }

    @Transactional
    public InquiryView requestEnd(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, true);
        requireStatus(item, "ACTIVE");
        item.setAnswererUnreadCount(0);
        item.setStatus("AWAITING_CONFIRMATION"); item.setConfirmationDeadline(LocalDateTime.now().plusHours(48));
        increaseUnread(item, item.getQuestioner().getId());
        notifications.send(item.getQuestioner(), "对方申请结束交流", notificationSubject(item) + "；请确认结束或继续交流", "/inquiries/" + item.getId());
        publishInquiryChanged(item.getQuestioner().getId(), item);
        return view(item, userId);
    }

    @Transactional
    public InquiryView continueChat(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, false);
        requireStatus(item, "AWAITING_CONFIRMATION");
        item.setQuestionerUnreadCount(0);
        item.setStatus("ACTIVE"); item.setConfirmationDeadline(null);
        increaseUnread(item, item.getAnswerer().getId());
        notifications.send(item.getAnswerer(), "对方选择继续交流", notificationSubject(item) + "；本次交流继续", "/inquiries/" + item.getId());
        publishInquiryChanged(item.getAnswerer().getId(), item);
        return view(item, userId);
    }

    @Transactional
    public InquiryView confirmEnd(Long userId, Long inquiryId) {
        Inquiry item = lockedParticipant(userId, inquiryId, false);
        if (!Set.of("ACTIVE", "AWAITING_CONFIRMATION").contains(item.getStatus()))
            throw BusinessException.badRequest("当前状态不能结束交流");
        item.setQuestionerUnreadCount(0);
        settle(item);
        publishInquiryChanged(item.getAnswerer().getId(), item);
        return view(item, userId);
    }

    @Scheduled(fixedDelayString = "${app.inquiry.timeout-scan-ms:60000}")
    @Transactional
    public void processTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        inquiries.findByStatusAndResponseDeadlineBefore("PENDING", now).forEach(i -> {
            Inquiry item = inquiries.findWithLockById(i.getId()).orElse(null);
            if (item != null && "PENDING".equals(item.getStatus())) {
                refund(item, "EXPIRED");
                increaseUnread(item, item.getQuestioner().getId());
                increaseUnread(item, item.getAnswerer().getId());
                notifications.send(item.getQuestioner(), "询问已超时", notificationSubject(item) + "；冻结金额已退回余额", "/inquiries/" + item.getId());
                notifications.send(item.getAnswerer(), "询问已超时", notificationSubject(item) + "；已自动关闭", "/inquiries/" + item.getId());
                publishInquiryChanged(item.getQuestioner().getId(), item);
                publishInquiryChanged(item.getAnswerer().getId(), item);
            }
        });
        inquiries.findByStatusAndConfirmationDeadlineBefore("AWAITING_CONFIRMATION", now).forEach(i -> {
            Inquiry item = inquiries.findWithLockById(i.getId()).orElse(null);
            if (item != null && "AWAITING_CONFIRMATION".equals(item.getStatus())) {
                settle(item);
                increaseUnread(item, item.getQuestioner().getId());
                notifications.send(item.getQuestioner(), "交流已自动结束", notificationSubject(item) + "；费用已支付给对方", "/inquiries/" + item.getId());
                publishInquiryChanged(item.getQuestioner().getId(), item);
                publishInquiryChanged(item.getAnswerer().getId(), item);
            }
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
        increaseUnread(item, item.getAnswerer().getId());
        notifications.send(item.getAnswerer(), "交流已结束", notificationSubject(item) + "；费用已进入账户余额", "/inquiries/" + item.getId());
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
    private Inquiry lockedAccessible(Long userId, Long id) {
        Inquiry item = inquiries.findWithLockById(id).orElseThrow(() -> BusinessException.notFound("询问不存在"));
        if (!item.getQuestioner().getId().equals(userId) && !item.getAnswerer().getId().equals(userId))
            throw BusinessException.forbidden("无权查看该询问");
        return item;
    }
    private void increaseUnread(Inquiry item, Long userId) {
        if (item.getQuestioner().getId().equals(userId)) {
            item.setQuestionerUnreadCount(item.getQuestionerUnreadCount() + 1);
        } else if (item.getAnswerer().getId().equals(userId)) {
            item.setAnswererUnreadCount(item.getAnswererUnreadCount() + 1);
        }
    }
    private int unreadFor(Inquiry item, Long userId) {
        return item.getQuestioner().getId().equals(userId)
            ? item.getQuestionerUnreadCount()
            : item.getAnswererUnreadCount();
    }
    private void publishInquiryChanged(Long userId, Inquiry item) {
        realtime.afterCommit(userId, "INQUIRY_UPDATED", new InquiryChangedEvent(
            item.getId(), item.getStatus(), item.getFundsStatus(), unreadFor(item, userId)
        ));
    }
    private User user(Long id) { return users.findById(id).orElseThrow(() -> BusinessException.notFound("用户不存在")); }
    private void requireStatus(Inquiry i, String status) { if (!status.equals(i.getStatus())) throw BusinessException.badRequest("当前状态不能执行该操作"); }
    private String required(String s, String message, int max) { if (s == null || s.isBlank()) throw BusinessException.badRequest(message); return clean(s, max); }
    private String clean(String s, int max) { if (s == null) return null; String v = s.trim(); return v.length() <= max ? v : v.substring(0, max); }
    private void validateChatImage(MultipartFile image) {
        if (image == null || image.isEmpty()) throw BusinessException.badRequest("请选择照片");
        if (image.getSize() > 10L * 1024 * 1024) throw BusinessException.badRequest("照片不能超过10MB");
        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw BusinessException.badRequest("只能发送照片");
        }
    }
    private String displayName(User user) {
        return user.getNickname() == null || user.getNickname().isBlank() ? "UID " + user.getUid() : user.getNickname();
    }
    private String notificationSubject(Inquiry item) {
        String question = item.getQuestion() == null ? "这次询问" : item.getQuestion().trim();
        return question.length() <= 36 ? question : question.substring(0, 36) + "…";
    }

    private InquiryView view(Inquiry i, Long me) {
        User other = i.getQuestioner().getId().equals(me) ? i.getAnswerer() : i.getQuestioner();
        return new InquiryView(i.getId(), i.getQuestioner().getId().equals(me) ? "QUESTIONER" : "ANSWERER",
                other.getId(), other.getNickname() == null || other.getNickname().isBlank() ? other.getUid() : other.getNickname(),
                other.getAvatarUrl(), i.getTopic(), i.getQuestion(), i.getAmount(), i.getStatus(), i.getFundsStatus(),
                unreadFor(i, me), i.getResponseDeadline(), i.getConfirmationDeadline(), i.getCreatedAt(),
                i.getLastMessageAt());
    }
    public record CreateCommand(Long answererId, String topic, String sourceType, String question, BigDecimal amount) {}
    public record InquiryView(Long id, String role, Long otherUserId, String otherName, String otherAvatar, String topic,
                              String question, BigDecimal amount, String status, String fundsStatus,
                              int unreadCount, LocalDateTime responseDeadline, LocalDateTime confirmationDeadline,
                              LocalDateTime createdAt, LocalDateTime lastMessageAt) {}
    public record MessageView(Long id, Long senderId, String senderName, String senderAvatar, String type, String content,
                              String attachmentUrl, String attachmentName, Long attachmentSize,
                              LocalDateTime createdAt) {
        static MessageView of(InquiryMessage m) { User s = m.getSender(); return new MessageView(m.getId(), s.getId(), s.getNickname() == null || s.getNickname().isBlank() ? s.getUid() : s.getNickname(), s.getAvatarUrl(), m.getMessageType(), m.getContent(), m.getAttachmentUrl(), m.getAttachmentName(), m.getAttachmentSize(), m.getCreatedAt()); }
    }
    public record InquiryDetail(InquiryView inquiry, List<MessageView> messages) {}
    public record InquiryChangedEvent(Long inquiryId, String status, String fundsStatus, int unreadCount) {}
    public record MessageEvent(Long inquiryId, int unreadCount, MessageView message) {}
}

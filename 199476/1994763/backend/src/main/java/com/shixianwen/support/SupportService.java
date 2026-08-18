package com.shixianwen.support;

import com.shixianwen.common.BusinessException;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StoredFile;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.realtime.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportService {
    private final FeedbackRecordRepository feedback;
    private final BusinessCooperationRepository cooperation;
    private final CustomerServiceMessageRepository customerService;
    private final UserRepository users;
    private final RealtimePublisher realtime;
    private final FileStorage fileStorage;
    private final SensitiveWordService sensitiveWords;

    @Transactional
    public FeedbackView feedback(Long userId, String type, String category, String content, Long targetUserId) {
        FeedbackRecord item = new FeedbackRecord();
        item.setUser(user(userId)); item.setFeedbackType(required(type)); item.setCategory(required(category));
        item.setContent(required(content));
        if (targetUserId != null) item.setTargetUser(user(targetUserId));
        return FeedbackView.of(feedback.save(item));
    }
    public List<FeedbackView> feedbackList(Long userId) { return feedback.findByUserIdOrderByCreatedAtDesc(userId).stream().map(FeedbackView::of).toList(); }
    @Transactional
    public Long cooperation(Long userId, String contact, String content) {
        BusinessCooperation item = new BusinessCooperation(); item.setUser(user(userId));
        item.setContact(required(contact)); item.setContent(required(content)); return cooperation.save(item).getId();
    }
    @Transactional
    public CustomerServiceView customerService(Long userId, String content) {
        CustomerServiceMessage item = new CustomerServiceMessage(); item.setUser(user(userId));
        item.setSenderType("USER"); item.setMessageType("TEXT"); item.setContent(required(content));
        return publishCustomerService(item);
    }
    @Transactional
    public CustomerServiceView customerServiceImage(Long userId, MultipartFile image) {
        if (image == null || image.isEmpty() || image.getContentType() == null || !image.getContentType().startsWith("image/")) {
            throw BusinessException.badRequest("请选择图片文件");
        }
        if (image.getSize() > 10L * 1024 * 1024) {
            throw BusinessException.badRequest("图片不能超过10MB");
        }
        StoredFile stored = fileStorage.store(image, "customer-service/" + userId);
        CustomerServiceMessage item = new CustomerServiceMessage();
        item.setUser(user(userId));
        item.setSenderType("USER");
        item.setMessageType("IMAGE");
        item.setContent("");
        item.setAttachmentUrl(stored.publicUrl());
        item.setAttachmentName(image.getOriginalFilename());
        item.setAttachmentSize(stored.size());
        return publishCustomerService(item);
    }
    private CustomerServiceView publishCustomerService(CustomerServiceMessage item) {
        item = customerService.saveAndFlush(item);
        CustomerServiceView view = CustomerServiceView.of(item);
        realtime.afterCommitToAdmins(
            "CUSTOMER_SERVICE_MESSAGE",
            new AdminCustomerServiceEvent(item.getUser().getId(), item.getUser().getUid(), item.getUser().getNickname(), item.getUser().getAvatarUrl(), view)
        );
        return view;
    }
    @Transactional
    public List<CustomerServiceView> customerServiceList(Long userId) {
        List<CustomerServiceMessage> items = customerService.findByUserIdOrderByCreatedAtAsc(userId);
        items.stream()
            .filter(item -> "SERVICE".equals(item.getSenderType()) && !item.isRead())
            .forEach(item -> item.setRead(true));
        return items.stream().map(CustomerServiceView::of).toList();
    }
    public long customerServiceUnreadCount(Long userId) {
        return customerService.countByUserIdAndSenderTypeAndReadFalse(userId, "SERVICE");
    }
    @Transactional
    public void readCustomerService(Long userId) {
        customerService.findByUserIdOrderByCreatedAtAsc(userId).stream()
            .filter(item -> "SERVICE".equals(item.getSenderType()) && !item.isRead())
            .forEach(item -> item.setRead(true));
    }
    private User user(Long id) { return users.findById(id).orElseThrow(() -> BusinessException.notFound("用户不存在")); }
    private String required(String value) { if (value == null || value.isBlank()) throw BusinessException.badRequest("内容不能为空"); return sensitiveWords.mask(value.trim()); }
    public record FeedbackView(Long id, String type, String category, String content, String status, java.time.LocalDateTime createdAt) {
        static FeedbackView of(FeedbackRecord i) { return new FeedbackView(i.getId(), i.getFeedbackType(), i.getCategory(), i.getContent(), i.getStatus(), i.getCreatedAt()); }
    }
    public record CustomerServiceView(Long id, String senderType, String messageType, String content, String attachmentUrl, String attachmentName, Long attachmentSize, java.time.LocalDateTime createdAt) {
        static CustomerServiceView of(CustomerServiceMessage i) { return new CustomerServiceView(i.getId(), i.getSenderType(), i.getMessageType(), i.getContent(), i.getAttachmentUrl(), i.getAttachmentName(), i.getAttachmentSize(), i.getCreatedAt()); }
    }
    public record AdminCustomerServiceEvent(Long userId, String uid, String nickname, String avatarUrl, CustomerServiceView message) {}
}

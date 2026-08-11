package com.shixianwen.support;

import com.shixianwen.common.BusinessException;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportService {
    private final FeedbackRecordRepository feedback;
    private final BusinessCooperationRepository cooperation;
    private final CustomerServiceMessageRepository customerService;
    private final UserRepository users;

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
        item.setSenderType("USER"); item.setContent(required(content));
        return CustomerServiceView.of(customerService.save(item));
    }
    public List<CustomerServiceView> customerServiceList(Long userId) { return customerService.findByUserIdOrderByCreatedAtAsc(userId).stream().map(CustomerServiceView::of).toList(); }
    private User user(Long id) { return users.findById(id).orElseThrow(() -> BusinessException.notFound("用户不存在")); }
    private String required(String value) { if (value == null || value.isBlank()) throw BusinessException.badRequest("内容不能为空"); return value.trim(); }
    public record FeedbackView(Long id, String type, String category, String content, String status, java.time.LocalDateTime createdAt) {
        static FeedbackView of(FeedbackRecord i) { return new FeedbackView(i.getId(), i.getFeedbackType(), i.getCategory(), i.getContent(), i.getStatus(), i.getCreatedAt()); }
    }
    public record CustomerServiceView(Long id, String senderType, String content, java.time.LocalDateTime createdAt) {
        static CustomerServiceView of(CustomerServiceMessage i) { return new CustomerServiceView(i.getId(), i.getSenderType(), i.getContent(), i.getCreatedAt()); }
    }
}

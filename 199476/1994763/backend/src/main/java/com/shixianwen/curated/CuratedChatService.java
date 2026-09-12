package com.shixianwen.curated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixianwen.admin.AdminAuditLog;
import com.shixianwen.admin.AdminAuditLogRepository;
import com.shixianwen.admin.AdminUser;
import com.shixianwen.common.BusinessException;
import com.shixianwen.config.AppGlobalSettingService;
import com.shixianwen.certification.Certification;
import com.shixianwen.certification.CertificationRepository;
import com.shixianwen.content.SensitiveContentCipher;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.realtime.RealtimePublisher;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.FileTypeDetector;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.storage.StoredFile;
import com.shixianwen.user.User;
import com.shixianwen.user.UserRepository;
import com.shixianwen.wallet.MockAlipayGateway;
import com.shixianwen.wallet.PaymentGateway;
import com.shixianwen.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.MovieHeaderBox;
import org.mp4parser.tools.Path;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.channels.Channels;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CuratedChatService {
    private static final int RING_SECONDS = 60;
    private static final String SAFETY_NOTICE = "安全提醒：请勿发送身份证号、银行卡号、家庭住址等敏感个人信息，也不要提供或引导交换手机号、微信等站外联系方式。平台外沟通无法获得平台记录与安全保障；经核实存在交换或引导交换站外联系方式的行为，将按1级违规处理。";
    private static final String GLOBAL_IDENTITY_STATUS_SQL = "COALESCE((SELECT c.status FROM certifications c WHERE c.user_id=a.user_id AND c.certification_type='IDENTITY' AND c.deleted_at IS NULL ORDER BY c.id DESC LIMIT 1),'NOT_APPLIED')";
    private static final String GLOBAL_IDENTITY_REASON_SQL = "(SELECT c.rejection_reason FROM certifications c WHERE c.user_id=a.user_id AND c.certification_type='IDENTITY' AND c.deleted_at IS NULL ORDER BY c.id DESC LIMIT 1)";

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final FileStorage storage;
    private final FileTypeDetector fileTypes;
    private final SensitiveWordService sensitiveWords;
    private final SensitiveContentCipher sensitiveCipher;
    private final PaymentGateway paymentGateway;
    private final WalletService walletService;
    private final RealtimePublisher realtime;
    private final AdminAuditLogRepository audits;
    private final ObjectMapper objectMapper;
    private final CertificationRepository certifications;
    private final AppGlobalSettingService globalSettings;
    @Value("${app.voice.ice-urls:stun:stun.l.google.com:19302}") private String iceUrls;
    @Value("${app.voice.turn-username:}") private String turnUsername;
    @Value("${app.voice.turn-credential:}") private String turnCredential;

    public MembershipView status(Long userId) {
        Map<String,Object> application = one("SELECT a.*,m.id membership_id,m.status membership_status,m.started_at,m.expires_at FROM curated_membership_applications a LEFT JOIN curated_memberships m ON m.user_id=a.user_id WHERE a.user_id=?", userId);
        if (application == null) {
            IdentitySnapshot identity = globalIdentity(userId);
            AppGlobalSettingService.Settings settings = globalSettings.current();
            return MembershipView.empty(
                identity.status(),
                identity.reason(),
                settings.curatedMembershipMonths(),
                settings.curatedMembershipPrice()
            );
        }
        expireIfNeeded(userId);
        application = one("SELECT a.*,m.id membership_id,m.status membership_status,m.started_at,m.expires_at FROM curated_membership_applications a LEFT JOIN curated_memberships m ON m.user_id=a.user_id WHERE a.user_id=?", userId);
        return membershipView(application);
    }

    @Transactional(readOnly = true)
    public MembershipQuoteView membershipQuote(Long userId) {
        requireGlobalIdentityApproved(userId);
        Map<String,Object> application = one(
            "SELECT a.job_status FROM curated_membership_applications a WHERE a.user_id=?",
            userId
        );
        if (application == null || !"APPROVED".equals(text(application.get("job_status")))) {
            throw BusinessException.badRequest("岗位认证通过后才可以开通");
        }
        AppGlobalSettingService.Settings settings = globalSettings.current();
        int months = settings.curatedMembershipMonths();
        BigDecimal price = settings.curatedMembershipPrice();
        LocalDateTime startsAt = LocalDateTime.now();
        LocalDateTime expiresAt = membershipExpiresAt(startsAt, months);
        return new MembershipQuoteView(
            price,
            months,
            membershipPriceText(price, months),
            startsAt,
            expiresAt,
            months == 1200,
            membershipValidityText(startsAt, expiresAt, months)
        );
    }

    @Transactional
    public MembershipView submitApplication(
        Long userId,
        List<MultipartFile> jobFiles
    ) {
        User user = activeUser(userId);
        int inserted=jdbc.update("INSERT IGNORE INTO curated_membership_applications(user_id) VALUES(?)",userId);
        Long id=jdbc.queryForObject("SELECT id FROM curated_membership_applications WHERE user_id=? FOR UPDATE",Long.class,userId);
        Map<String,Object> current = one("SELECT job_status FROM curated_membership_applications WHERE id=?", id);
        boolean submitJob = inserted == 1 || "REJECTED".equals(text(current.get("job_status")));
        List<MultipartFile> suppliedJobFiles = jobFiles == null
            ? List.of()
            : jobFiles.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (!submitJob && !suppliedJobFiles.isEmpty()) {
            throw BusinessException.badRequest("岗位认证正在审核或已通过，仅可查看");
        }
        if (!submitJob) {
            throw BusinessException.badRequest("岗位认证正在审核或已通过，仅可查看");
        }
        List<MultipartFile> validatedJobFiles = validateJobFiles(suppliedJobFiles);
        if (inserted == 0) {
            jdbc.update("UPDATE curated_membership_materials SET deleted_at=NOW(6) WHERE application_id=? AND material_type='JOB' AND deleted_at IS NULL", id);
            jdbc.update("UPDATE curated_membership_applications SET job_status='PENDING',job_rejection_reason=NULL,job_title=NULL,job_years=NULL,job_reviewed_by=NULL,job_reviewed_at=NULL WHERE id=?", id);
        }
        for (MultipartFile file : validatedJobFiles) storeMaterial(id, "JOB", file, false);
        realtime.afterCommitToAdmins("CURATED_APPLICATION_UPDATED", Map.of("applicationId", id, "userId", userId));
        return status(user.getId());
    }

    public List<MaterialView> myMaterials(Long userId) {
        return materialsFor("SELECT m.* FROM curated_membership_materials m JOIN curated_membership_applications a ON a.id=m.application_id WHERE a.user_id=? AND m.material_type='JOB' AND m.deleted_at IS NULL ORDER BY m.id", userId);
    }

    @Transactional
    public PaymentView createPayment(
        Long userId,
        String requestId,
        Integer quotedDurationMonths,
        BigDecimal quotedPrice
    ) {
        requireRequestId(requestId);
        requireGlobalIdentityApproved(userId);
        AppGlobalSettingService.Settings settings = globalSettings.current();
        int durationMonths = settings.curatedMembershipMonths();
        BigDecimal price = settings.curatedMembershipPrice();
        if (quotedDurationMonths != null && quotedDurationMonths != durationMonths) {
            throw BusinessException.badRequest("开通期限已更新，请重新确认");
        }
        if (quotedPrice != null && quotedPrice.compareTo(price) != 0) {
            throw BusinessException.badRequest("开通金额已更新，请重新确认");
        }
        Map<String,Object> application = one("SELECT a.id application_id,m.id membership_id,m.status,m.expires_at FROM curated_membership_applications a LEFT JOIN curated_memberships m ON m.user_id=a.user_id WHERE a.user_id=? AND a.job_status='APPROVED' FOR UPDATE", userId);
        if (application == null) throw BusinessException.badRequest("岗位认证通过后才可以开通");
        Long membershipId = number(application.get("membership_id"));
        if (membershipId == null) {
            jdbc.update("INSERT INTO curated_memberships(user_id,application_id,status) VALUES(?,?,'INACTIVE')", userId, number(application.get("application_id")));
            membershipId = jdbc.queryForObject("SELECT id FROM curated_memberships WHERE user_id=?", Long.class, userId);
        }
        Map<String,Object> active = one("SELECT status,expires_at FROM curated_memberships WHERE id=?", membershipId);
        if (active != null && "ACTIVE".equals(text(active.get("status"))) && date(active.get("expires_at")) != null && date(active.get("expires_at")).isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("严选直聊当前仍在有效期内");
        }
        Map<String,Object> existing = one("SELECT * FROM curated_membership_orders WHERE user_id=? AND request_no=?", userId, requestId);
        if (existing != null) {
            if ("PAID".equals(text(existing.get("status")))) recordMembershipPayment(existing);
            return paymentView(existing, existingPaymentPayload(existing));
        }
        Map<String,Object> pending=one("SELECT * FROM curated_membership_orders WHERE user_id=? AND status='PENDING' AND duration_months=? AND amount=? ORDER BY id DESC LIMIT 1",userId,durationMonths,price);
        if(pending!=null) return paymentView(pending,existingPaymentPayload(pending));
        User user = activeUser(userId);
        boolean test = "TEST".equals(user.getAccountType());
        String orderNo = "RXL" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String status = test ? "PAID" : "PENDING";
        jdbc.update("INSERT INTO curated_membership_orders(user_id,membership_id,request_no,order_no,channel,amount,duration_months,status,paid_at) VALUES(?,?,?,?,?,?,?,?,?)",
            userId, membershipId, requestId, orderNo, test ? "TEST" : "ALIPAY", price, durationMonths, status, test ? Timestamp.valueOf(LocalDateTime.now()) : null);
        if (test) {
            Map<String,Object> paidOrder = one("SELECT * FROM curated_membership_orders WHERE order_no=?", orderNo);
            recordMembershipPayment(paidOrder);
            activateMembership(membershipId, LocalDateTime.now(), durationMonths);
            return paymentView(paidOrder, null);
        }
        if (!paymentGateway.capability().available()) throw BusinessException.serviceUnavailable(paymentGateway.capability().message());
        PaymentGateway.PaymentOrder order = paymentGateway.createOrder(orderNo, price, membershipPaymentSubject(durationMonths));
        return paymentView(one("SELECT * FROM curated_membership_orders WHERE order_no=?", orderNo), order.paymentPayload());
    }

    @Transactional
    public PaymentView payment(Long userId, String orderNo) {
        Map<String,Object> item = one("SELECT * FROM curated_membership_orders WHERE user_id=? AND order_no=?", userId, orderNo);
        if (item == null) throw BusinessException.notFound("开通订单不存在");
        if ("PENDING".equals(text(item.get("status")))) {
            PaymentGateway.PaymentStatus queried = paymentGateway.queryOrder(orderNo);
            if ("PAID".equals(queried.status())) applyPaid(queried.orderNo(), queried.providerTradeNo(), queried.paidAmount(), queried.paidAt());
            item = one("SELECT * FROM curated_membership_orders WHERE user_id=? AND order_no=?", userId, orderNo);
        }
        if ("PAID".equals(text(item.get("status")))) recordMembershipPayment(item);
        return paymentView(item, null);
    }

    @Transactional
    public void applyPaid(PaymentGateway.PaymentNotification notice) {
        if (!"PAID".equals(notice.status())) return;
        applyPaid(notice.orderNo(), notice.providerTradeNo(), notice.paidAmount(), notice.paidAt());
    }

    @Transactional
    public void completeMockPayment(String orderNo) {
        ensureMockPayment();
        Map<String,Object> order = one("SELECT amount FROM curated_membership_orders WHERE order_no=?", orderNo);
        if (order == null) throw BusinessException.notFound("开通订单不存在");
        applyPaid(orderNo, null, new BigDecimal(text(order.get("amount"))), LocalDateTime.now());
    }

    public PaymentView mockOrder(String orderNo) {
        ensureMockPayment();
        Map<String,Object> item = one("SELECT * FROM curated_membership_orders WHERE order_no=?", orderNo);
        if (item == null) throw BusinessException.notFound("开通订单不存在");
        return paymentView(item, null);
    }

    public PageView<MemberView> onlineMembers(Long userId, String keyword, int page, int size) {
        requireActiveMembership(userId);
        String q = keyword == null ? "" : keyword.trim();
        List<Map<String,Object>> candidates = jdbc.queryForList("SELECT u.id,u.uid,u.nickname,u.avatar_url,a.job_title,a.job_years FROM curated_memberships m JOIN curated_membership_applications a ON a.id=m.application_id JOIN users u ON u.id=m.user_id WHERE m.status='ACTIVE' AND m.expires_at>? AND u.account_status='ACTIVE' AND u.id<>? AND (a.job_title LIKE ? OR u.uid LIKE ? OR COALESCE(u.nickname,'') LIKE ?) AND NOT EXISTS(SELECT 1 FROM curated_chat_blocks b WHERE (b.blocker_id=u.id AND b.blocked_id=?) OR (b.blocker_id=? AND b.blocked_id=u.id)) ORDER BY m.started_at DESC",
            Timestamp.valueOf(LocalDateTime.now()), userId, "%"+q+"%", "%"+q+"%", "%"+q+"%", userId, userId);
        List<MemberView> members = candidates.stream().map(this::memberView).toList();
        int from = Math.min(page * size, members.size()); int to = Math.min(from + size, members.size());
        return new PageView<>(members.subList(from,to), members.size(), page, size, to < members.size());
    }

    @Transactional
    public ConversationView openConversation(Long userId, Long otherUserId) {
        if (Objects.equals(userId, otherUserId)) throw BusinessException.badRequest("不能与自己创建聊天");
        requireActiveMembership(userId); requireActiveMembership(otherUserId); requireNotBlocked(userId, otherUserId);
        if (!realtime.isUserOnline(otherUserId)) throw BusinessException.badRequest("对方当前不在线");
        long low = Math.min(userId, otherUserId), high = Math.max(userId, otherUserId);
        Long id = jdbc.query("SELECT id FROM curated_conversations WHERE user_low_id=? AND user_high_id=?", (rs,n)->rs.getLong(1), low, high).stream().findFirst().orElse(null);
        if (id == null) {
            try {
                KeyHolder key = new GeneratedKeyHolder();
                jdbc.update(c -> { PreparedStatement ps=c.prepareStatement("INSERT INTO curated_conversations(user_low_id,user_high_id) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);ps.setLong(1,low);ps.setLong(2,high);return ps;}, key);
                id = Objects.requireNonNull(key.getKey()).longValue();
                jdbc.update("INSERT INTO curated_messages(conversation_id,sender_id,message_type,content) VALUES(?,NULL,'SYSTEM',?)", id, SAFETY_NOTICE);
                jdbc.update("INSERT INTO curated_conversation_reads(conversation_id,user_id) VALUES(?,?),(?,?)", id, low, id, high);
            } catch (DataIntegrityViolationException duplicate) {
                id = jdbc.queryForObject("SELECT id FROM curated_conversations WHERE user_low_id=? AND user_high_id=?", Long.class, low, high);
            }
        }
        return conversation(userId, id, true);
    }

    public List<ConversationView> conversations(Long userId) {
        expireIfNeeded(userId);
        return jdbc.queryForList("SELECT id FROM curated_conversations WHERE user_low_id=? OR user_high_id=? ORDER BY last_message_at DESC", userId, userId).stream().map(r -> conversation(userId, number(r.get("id")), false)).toList();
    }

    public long unreadCount(Long userId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM curated_messages m JOIN curated_conversations c ON c.id=m.conversation_id LEFT JOIN curated_conversation_reads r ON r.conversation_id=c.id AND r.user_id=? WHERE (c.user_low_id=? OR c.user_high_id=?) AND m.sender_id IS NOT NULL AND m.sender_id<>? AND m.id>COALESCE(r.last_read_message_id,0)", Long.class, userId,userId,userId,userId);
        return count == null ? 0 : count;
    }

    public ConversationView conversation(Long userId, Long id, boolean includeMessages) {
        Map<String,Object> row = one("SELECT c.*,CASE WHEN c.user_low_id=? THEN c.user_high_id ELSE c.user_low_id END other_id FROM curated_conversations c WHERE c.id=? AND (c.user_low_id=? OR c.user_high_id=?)", userId,id,userId,userId);
        if (row == null) throw BusinessException.notFound("聊天不存在");
        Long otherId=number(row.get("other_id"));
        Map<String,Object> user=one("SELECT u.id,u.uid,u.nickname,u.avatar_url,a.job_title,a.job_years,m.status membership_status,m.expires_at FROM users u JOIN curated_membership_applications a ON a.user_id=u.id JOIN curated_memberships m ON m.user_id=u.id WHERE u.id=?",otherId);
        long unread = Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM curated_messages m LEFT JOIN curated_conversation_reads r ON r.conversation_id=m.conversation_id AND r.user_id=? WHERE m.conversation_id=? AND m.sender_id IS NOT NULL AND m.sender_id<>? AND m.id>COALESCE(r.last_read_message_id,0)",Long.class,userId,id,userId)).orElse(0L);
        MessageView last = jdbc.query("SELECT * FROM curated_messages WHERE conversation_id=? ORDER BY id DESC LIMIT 1", (rs,n)->messageView(userId,map(rs)), id).stream().findFirst().orElse(null);
        List<MessageView> messages = includeMessages ? messages(userId,id,0,0,100) : List.of();
        boolean writable=isMembershipActive(userId)&&isMembershipActive(otherId)&&!blocked(userId,otherId);
        return new ConversationView(id,memberView(user),unread,last,messages,writable,date(row.get("last_message_at")));
    }

    public List<MessageView> messages(Long userId, Long conversationId, long afterId, long beforeId, int limit) {
        requireParticipant(userId, conversationId);
        int safeLimit=Math.min(Math.max(limit,1),200);
        if(afterId>0&&beforeId>0) throw BusinessException.badRequest("消息查询条件无效");
        if(afterId>0) return jdbc.query("SELECT * FROM curated_messages WHERE conversation_id=? AND id>? ORDER BY id ASC LIMIT ?", (rs,n)->messageView(userId,map(rs)), conversationId,afterId,safeLimit);
        List<MessageView> result=beforeId>0
            ?jdbc.query("SELECT * FROM curated_messages WHERE conversation_id=? AND id<? ORDER BY id DESC LIMIT ?",(rs,n)->messageView(userId,map(rs)),conversationId,beforeId,safeLimit)
            :jdbc.query("SELECT * FROM curated_messages WHERE conversation_id=? ORDER BY id DESC LIMIT ?",(rs,n)->messageView(userId,map(rs)),conversationId,safeLimit);
        Collections.reverse(result);
        return result;
    }

    @Transactional
    public void read(Long userId, Long conversationId) {
        requireParticipant(userId,conversationId);
        Long last=jdbc.queryForObject("SELECT MAX(id) FROM curated_messages WHERE conversation_id=?",Long.class,conversationId);
        jdbc.update("INSERT INTO curated_conversation_reads(conversation_id,user_id,last_read_message_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE last_read_message_id=VALUES(last_read_message_id)",conversationId,userId,last);
        realtime.afterCommit(userId,"CURATED_UNREAD_UPDATED",Map.of("count",unreadCount(userId)));
    }

    @Transactional
    public MessageView sendText(Long userId, Long conversationId, String raw) {
        String content=raw==null?"":raw.trim();
        if(content.isEmpty()||content.codePointCount(0,content.length())>2000) throw BusinessException.badRequest("消息内容应为1至2000个字");
        Long other=requireWritable(userId,conversationId);
        String masked=sensitiveWords.mask(content);
        Long id=insertMessage(conversationId,userId,"TEXT",masked,sensitiveCipher.encrypt(content),null,null,null);
        return publishedMessage(userId,other,conversationId,id);
    }

    @Transactional
    public MessageView sendImage(Long userId, Long conversationId, MultipartFile image) {
        Long other=requireWritable(userId,conversationId);
        if(image.getSize()>20L*1024*1024) throw BusinessException.badRequest("图片不能超过20MB");
        fileTypes.requireImage(image);
        StoredFile stored=storage.store(image,"curated-chat/"+conversationId,StorageVisibility.PRIVATE);
        Long id=insertMessage(conversationId,userId,"IMAGE","[图片]",null,stored.storageKey(),safeName(image.getOriginalFilename()),stored.size());
        return publishedMessage(userId,other,conversationId,id);
    }

    @Transactional
    public void block(Long userId, Long conversationId) {
        Long other=requireParticipant(userId,conversationId);
        jdbc.update("INSERT IGNORE INTO curated_chat_blocks(blocker_id,blocked_id) VALUES(?,?)",userId,other);
        realtime.afterCommit(other,"CURATED_CHAT_BLOCKED",Map.of("conversationId",conversationId));
    }

    @Transactional
    public VoiceCallView startCall(Long userId, Long conversationId) {
        Long other=requireWritable(userId,conversationId);
        if(!realtime.isUserOnline(other)) throw BusinessException.badRequest("对方当前不在线，暂时无法发起语音通话");
        // 串行化同一聊天中的发起动作，避免双方同时呼叫时创建两条有效通话。
        jdbc.queryForObject("SELECT id FROM curated_conversations WHERE id=? FOR UPDATE",Long.class,conversationId);
        Integer active=jdbc.queryForObject("SELECT COUNT(*) FROM curated_voice_calls WHERE conversation_id=? AND status IN ('RINGING','ANSWERED','CONNECTED')",Integer.class,conversationId);
        if(active!=null&&active>0) throw BusinessException.badRequest("当前已有一通语音正在进行");
        LocalDateTime now=LocalDateTime.now(),deadline=now.plusSeconds(RING_SECONDS);
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO curated_voice_calls(conversation_id,caller_id,callee_id,status,connect_deadline) VALUES(?,?,?,'RINGING',?)",Statement.RETURN_GENERATED_KEYS);ps.setLong(1,conversationId);ps.setLong(2,userId);ps.setLong(3,other);ps.setTimestamp(4,Timestamp.valueOf(deadline));return ps;},key);
        Long id=Objects.requireNonNull(key.getKey()).longValue();
        realtime.afterCommit(other,"CURATED_VOICE_RINGING",Map.of("conversationId",conversationId,"callId",id,"callerId",userId));
        return voiceCall(userId,id);
    }

    @Transactional
    public VoiceCallView answerCall(Long userId, Long callId) {Map<String,Object> call=lockedCall(callId);if(!Objects.equals(number(call.get("callee_id")),userId))throw BusinessException.forbidden("只有被呼叫方可以接听");requireCallStatus(call,"RINGING");if(LocalDateTime.now().isAfter(date(call.get("connect_deadline")))){endCallRow(callId,"MISSED",null);throw BusinessException.badRequest("本次语音来电已结束");}jdbc.update("UPDATE curated_voice_calls SET status='ANSWERED',answered_at=NOW(6) WHERE id=?",callId);realtime.afterCommit(number(call.get("caller_id")),"CURATED_VOICE_ANSWERED",Map.of("conversationId",number(call.get("conversation_id")),"callId",callId));return voiceCall(userId,callId);}
    @Transactional
    public VoiceCallView rejectCall(Long userId, Long callId) {Map<String,Object> call=lockedCall(callId);if(!Objects.equals(number(call.get("callee_id")),userId))throw BusinessException.forbidden("只有被呼叫方可以拒绝");requireCallStatus(call,"RINGING");endCallRow(callId,"REJECTED",userId);publishCallEnded(call,"CURATED_VOICE_REJECTED");return voiceCall(userId,callId);}
    @Transactional
    public VoiceCallView connected(Long userId, Long callId) {Map<String,Object> call=lockedCall(callId);requireCallParticipant(call,userId);if(!List.of("ANSWERED","CONNECTED").contains(text(call.get("status"))))throw BusinessException.badRequest("当前语音状态不能建立连接");jdbc.update("UPDATE curated_voice_calls SET status='CONNECTED',connected_at=COALESCE(connected_at,NOW(6)) WHERE id=?",callId);return voiceCall(userId,callId);}
    @Transactional
    public VoiceCallView endCall(Long userId, Long callId) {Map<String,Object> call=lockedCall(callId);requireCallParticipant(call,userId);if(List.of("ENDED","REJECTED","MISSED").contains(text(call.get("status"))))return voiceCall(userId,callId);endCallRow(callId,"ENDED",userId);publishCallEnded(call,"CURATED_VOICE_ENDED");return voiceCall(userId,callId);}
    public VoiceCallView call(Long userId,Long callId){return voiceCall(userId,callId);}
    public IceConfig iceConfig(Long userId,Long callId){Map<String,Object> call=one("SELECT * FROM curated_voice_calls WHERE id=?",callId);if(call==null)throw BusinessException.notFound("语音通话不存在");requireCallParticipant(call,userId);List<String> urls=Arrays.stream(iceUrls.split(",")).map(String::trim).filter(x->!x.isBlank()).toList();return new IceConfig(urls.isEmpty()?List.of("stun:stun.l.google.com:19302"):urls,turnUsername,turnCredential);}

    @Transactional
    public SignalView signal(Long userId,Long callId,String type,String payload){Map<String,Object> call=lockedCall(callId);Long other=requireCallParticipant(call,userId);if(!List.of("RINGING","ANSWERED","CONNECTED").contains(text(call.get("status"))))throw BusinessException.badRequest("语音通话已经结束");String normalized=type==null?"":type.trim().toUpperCase(Locale.ROOT);if(!Set.of("OFFER","ANSWER","ICE").contains(normalized))throw BusinessException.badRequest("语音信令类型无效");if(payload==null||payload.length()>100_000)throw BusinessException.badRequest("语音信令内容无效");try{objectMapper.readTree(payload);}catch(Exception e){throw BusinessException.badRequest("语音信令内容无效");}KeyHolder key=new GeneratedKeyHolder();jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO curated_voice_signals(call_id,sender_id,recipient_id,signal_type,payload_json) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);ps.setLong(1,callId);ps.setLong(2,userId);ps.setLong(3,other);ps.setString(4,normalized);ps.setString(5,payload);return ps;},key);Long id=Objects.requireNonNull(key.getKey()).longValue();realtime.afterCommit(other,"CURATED_VOICE_SIGNAL",Map.of("callId",callId,"signalId",id));return new SignalView(id,normalized,payload,LocalDateTime.now());}
    @Transactional
    public List<SignalView> signals(Long userId,Long callId,long afterId){Map<String,Object> call=lockedCall(callId);requireCallParticipant(call,userId);List<SignalView> result=jdbc.query("SELECT * FROM curated_voice_signals WHERE call_id=? AND recipient_id=? AND id>? ORDER BY id",(rs,n)->new SignalView(rs.getLong("id"),rs.getString("signal_type"),rs.getString("payload_json"),rs.getTimestamp("created_at").toLocalDateTime()),callId,userId,afterId);if(!result.isEmpty())jdbc.update("UPDATE curated_voice_signals SET consumed_at=NOW(6) WHERE call_id=? AND recipient_id=? AND id<=?",callId,userId,result.get(result.size()-1).id());return result;}

    @Scheduled(fixedDelay=15_000L)
    @Transactional
    public void expireCallsAndMemberships(){List<Map<String,Object>> calls=jdbc.queryForList("SELECT * FROM curated_voice_calls WHERE status='RINGING' AND connect_deadline<NOW(6)");for(Map<String,Object> call:calls){endCallRow(number(call.get("id")),"MISSED",null);publishCallEnded(call,"CURATED_VOICE_MISSED");}jdbc.update("UPDATE curated_memberships SET status='EXPIRED' WHERE status='ACTIVE' AND expires_at<=NOW(6)");}

    public List<Map<String,Object>> adminApplications(String keyword,String status,int page,int size){String q="%"+(keyword==null?"":keyword.trim())+"%";String statusSql=(status==null||status.isBlank())?"":" AND ("+GLOBAL_IDENTITY_STATUS_SQL+"=? OR a.job_status=?)";List<Object> args=new ArrayList<>(List.of(q,q,q));if(!statusSql.isEmpty()){args.add(status);args.add(status);}args.add(size);args.add(page*size);String sql="SELECT a.id,a.user_id,u.uid,u.phone,u.nickname,u.avatar_url,"+GLOBAL_IDENTITY_STATUS_SQL+" identity_status,a.job_status,"+GLOBAL_IDENTITY_REASON_SQL+" identity_rejection_reason,a.job_rejection_reason,a.job_title,a.job_years,a.created_at,a.updated_at,m.status membership_status,m.started_at,m.expires_at FROM curated_membership_applications a JOIN users u ON u.id=a.user_id LEFT JOIN curated_memberships m ON m.user_id=a.user_id WHERE (u.uid LIKE ? OR u.phone LIKE ? OR COALESCE(u.nickname,'') LIKE ?)"+statusSql+" ORDER BY a.updated_at DESC LIMIT ? OFFSET ?";return jdbc.queryForList(sql,args.toArray());}
    public long adminApplicationCount(String keyword,String status){String q="%"+(keyword==null?"":keyword.trim())+"%";if(status==null||status.isBlank())return jdbc.queryForObject("SELECT COUNT(*) FROM curated_membership_applications a JOIN users u ON u.id=a.user_id WHERE u.uid LIKE ? OR u.phone LIKE ? OR COALESCE(u.nickname,'') LIKE ?",Long.class,q,q,q);return jdbc.queryForObject("SELECT COUNT(*) FROM curated_membership_applications a JOIN users u ON u.id=a.user_id WHERE (u.uid LIKE ? OR u.phone LIKE ? OR COALESCE(u.nickname,'') LIKE ?) AND ("+GLOBAL_IDENTITY_STATUS_SQL+"=? OR a.job_status=?)",Long.class,q,q,q,status,status);}
    public List<MaterialView> adminMaterials(Long applicationId){if(one("SELECT id FROM curated_membership_applications WHERE id=?",applicationId)==null)throw BusinessException.notFound("严选直聊申请不存在");return materialsFor("SELECT * FROM curated_membership_materials WHERE application_id=? AND material_type='JOB' AND deleted_at IS NULL ORDER BY id",applicationId);}

    @Transactional
    public void reviewJob(AdminUser admin,Long id,boolean approved,String jobTitle,Integer jobYears,String reason,String ip){Map<String,Object> app=lockApplication(id);if(approved){String title=requireText(jobTitle,2,80,"岗位名称");if(jobYears==null||jobYears<10||jobYears>80)throw BusinessException.badRequest("岗位年限必须在10至80年之间");jdbc.update("UPDATE curated_membership_applications SET job_status='APPROVED',job_rejection_reason=NULL,job_title=?,job_years=?,job_reviewed_by=?,job_reviewed_at=NOW(6) WHERE id=?",title,jobYears,admin.getId(),id);}else{jdbc.update("UPDATE curated_membership_applications SET job_status='REJECTED',job_rejection_reason=?,job_title=NULL,job_years=NULL,job_reviewed_by=?,job_reviewed_at=NOW(6) WHERE id=?",requireReason(reason),admin.getId(),id);}audit(admin,"CURATED_JOB_REVIEW",id,approved?"通过":"驳回："+reason,ip);notifyReview(number(app.get("user_id")),id);}
    @Transactional
    public void suspendMembership(AdminUser admin,Long userId,boolean active,String reason,String ip){Map<String,Object> m=one("SELECT * FROM curated_memberships WHERE user_id=? FOR UPDATE",userId);if(m==null)throw BusinessException.notFound("严选直聊会员不存在");if(active){if(date(m.get("expires_at"))==null||!date(m.get("expires_at")).isAfter(LocalDateTime.now()))throw BusinessException.badRequest("会员已经到期，不能直接恢复");jdbc.update("UPDATE curated_memberships SET status='ACTIVE' WHERE user_id=?",userId);}else jdbc.update("UPDATE curated_memberships SET status='SUSPENDED' WHERE user_id=?",userId);audit(admin,active?"CURATED_MEMBER_RESTORE":"CURATED_MEMBER_SUSPEND",userId,reason,ip);realtime.afterCommit(userId,"CURATED_MEMBERSHIP_UPDATED",Map.of());}

    private void applyPaid(String orderNo,String tradeNo,BigDecimal paidAmount,LocalDateTime paidAt){Map<String,Object> order=one("SELECT * FROM curated_membership_orders WHERE order_no=? FOR UPDATE",orderNo);if(order==null)throw BusinessException.notFound("开通订单不存在");if("PAID".equals(text(order.get("status")))){recordMembershipPayment(order);return;}if(!"PENDING".equals(text(order.get("status"))))throw BusinessException.badRequest("开通订单当前不可支付");BigDecimal orderAmount=new BigDecimal(text(order.get("amount")));if(paidAmount==null||paidAmount.compareTo(orderAmount)!=0)throw BusinessException.badRequest("支付金额与开通订单不一致");if(tradeNo!=null&&!tradeNo.isBlank()){Map<String,Object> duplicate=one("SELECT id FROM curated_membership_orders WHERE provider_trade_no=? AND order_no<>?",tradeNo,orderNo);if(duplicate!=null)throw BusinessException.badRequest("支付宝交易号已被其他订单使用");}LocalDateTime effectivePaid=paidAt==null?LocalDateTime.now():paidAt;jdbc.update("UPDATE curated_membership_orders SET status='PAID',provider_trade_no=?,paid_at=? WHERE id=?",tradeNo,Timestamp.valueOf(effectivePaid),number(order.get("id")));recordMembershipPayment(order);activateMembership(number(order.get("membership_id")),effectivePaid,integer(order.get("duration_months")));}
    private void recordMembershipPayment(Map<String,Object> order){walletService.recordCuratedMembershipPurchase(number(order.get("user_id")),number(order.get("id")),new BigDecimal(text(order.get("amount"))),"TEST".equals(text(order.get("channel"))));}
    private void activateMembership(Long membershipId,LocalDateTime paidAt,int durationMonths){Map<String,Object> m=one("SELECT * FROM curated_memberships WHERE id=? FOR UPDATE",membershipId);LocalDateTime existing=date(m.get("expires_at"));LocalDateTime start=existing!=null&&existing.isAfter(paidAt)?existing:paidAt;LocalDateTime end=membershipExpiresAt(start,durationMonths);jdbc.update("UPDATE curated_memberships SET status='ACTIVE',started_at=?,expires_at=?,duration_months=? WHERE id=?",Timestamp.valueOf(start),Timestamp.valueOf(end),durationMonths,membershipId);Long userId=number(m.get("user_id"));realtime.afterCommit(userId,"CURATED_MEMBERSHIP_UPDATED",Map.of("expiresAt",end.toString(),"durationMonths",durationMonths));}
    private String existingPaymentPayload(Map<String,Object> existing){if("PAID".equals(text(existing.get("status"))))return null;int months=integer(existing.get("duration_months"));return paymentGateway.createOrder(text(existing.get("order_no")),new BigDecimal(text(existing.get("amount"))),membershipPaymentSubject(months)).paymentPayload();}
    private PaymentView paymentView(Map<String,Object> row,String payload){int months=integer(row.get("duration_months"));BigDecimal price=new BigDecimal(text(row.get("amount")));return new PaymentView(number(row.get("id")),text(row.get("order_no")),price,months,membershipPriceText(price,months),text(row.get("status")),text(row.get("channel")),payload,date(row.get("paid_at")),date(row.get("created_at")));}
    private MembershipView membershipView(Map<String,Object> row){IdentitySnapshot identity=globalIdentity(number(row.get("user_id")));String job=text(row.get("job_status")),membership=text(row.get("membership_status"));LocalDateTime expires=date(row.get("expires_at"));String overall;if("ACTIVE".equals(membership)&&expires!=null&&expires.isAfter(LocalDateTime.now()))overall="ACTIVE";else if("SUSPENDED".equals(membership))overall="SUSPENDED";else if("APPROVED".equals(identity.status())&&"APPROVED".equals(job))overall="READY_TO_PAY";else if("REJECTED".equals(identity.status())||"REJECTED".equals(job))overall="REJECTED";else overall="UNDER_REVIEW";AppGlobalSettingService.Settings settings=globalSettings.current();int months=settings.curatedMembershipMonths();BigDecimal price=settings.curatedMembershipPrice();return new MembershipView(true,overall,identity.status(),job,identity.reason(),text(row.get("job_rejection_reason")),text(row.get("job_title")),integer(row.get("job_years")),date(row.get("started_at")),expires,price,months,membershipPriceText(price,months));}
    static LocalDateTime membershipExpiresAt(LocalDateTime startsAt,int months){if(months==1200||startsAt.getYear()>=9999)return LocalDateTime.of(9999,12,31,23,59,59);return startsAt.plusMonths(months);}
    static String membershipTermLabel(int months){return switch(months){case 1->"月";case 6->"半年";case 12->"年";case 1200->"永久";default->months+"个月";};}
    static String membershipPriceText(BigDecimal price,int months){return price.stripTrailingZeros().toPlainString()+"元/"+membershipTermLabel(months);}
    static String membershipPaymentSubject(int months){return "严选直聊"+membershipTermLabel(months)+"使用权";}
    static String membershipValidityText(LocalDateTime startsAt,LocalDateTime expiresAt,int months){DateTimeFormatter formatter=DateTimeFormatter.ofPattern("yyyy.M.d HH:mm");return months==1200?startsAt.format(formatter)+" 起，永久有效":startsAt.format(formatter)+"\n至 "+expiresAt.format(formatter);}
    private void expireIfNeeded(Long userId){jdbc.update("UPDATE curated_memberships SET status='EXPIRED' WHERE user_id=? AND status='ACTIVE' AND expires_at<=NOW(6)",userId);}
    private boolean isMembershipActive(Long userId){expireIfNeeded(userId);Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM curated_memberships m JOIN users u ON u.id=m.user_id WHERE m.user_id=? AND m.status='ACTIVE' AND m.expires_at>NOW(6) AND u.account_status='ACTIVE'",Integer.class,userId);return count!=null&&count>0;}
    private void requireActiveMembership(Long userId){if(!isMembershipActive(userId))throw BusinessException.forbidden("严选直聊当前未开通或已到期");}
    private User activeUser(Long id){return users.findById(id).filter(u->"ACTIVE".equals(u.getAccountStatus())).orElseThrow(()->BusinessException.forbidden("账号当前不可用"));}
    private void requireNotBlocked(Long a,Long b){if(blocked(a,b))throw BusinessException.forbidden("双方已无法继续交流");}
    private boolean blocked(Long a,Long b){Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM curated_chat_blocks WHERE (blocker_id=? AND blocked_id=?) OR (blocker_id=? AND blocked_id=?)",Integer.class,a,b,b,a);return count!=null&&count>0;}
    private Long requireParticipant(Long userId,Long conversationId){Map<String,Object> row=one("SELECT user_low_id,user_high_id FROM curated_conversations WHERE id=?",conversationId);if(row==null)throw BusinessException.notFound("聊天不存在");Long low=number(row.get("user_low_id")),high=number(row.get("user_high_id"));if(!Objects.equals(userId,low)&&!Objects.equals(userId,high))throw BusinessException.forbidden("无权查看该聊天");return Objects.equals(userId,low)?high:low;}
    private Long requireWritable(Long userId,Long conversationId){Long other=requireParticipant(userId,conversationId);requireActiveMembership(userId);requireActiveMembership(other);requireNotBlocked(userId,other);return other;}
    private Long insertMessage(Long conversationId,Long sender,String type,String content,String raw,String key,String name,Long size){KeyHolder holder=new GeneratedKeyHolder();jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO curated_messages(conversation_id,sender_id,message_type,content,raw_content_encrypted,attachment_key,attachment_name,attachment_size) VALUES(?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);ps.setLong(1,conversationId);ps.setLong(2,sender);ps.setString(3,type);ps.setString(4,content);ps.setString(5,raw);ps.setString(6,key);ps.setString(7,name);if(size==null)ps.setNull(8,java.sql.Types.BIGINT);else ps.setLong(8,size);return ps;},holder);Long id=Objects.requireNonNull(holder.getKey()).longValue();jdbc.update("UPDATE curated_conversations SET last_message_at=NOW(6) WHERE id=?",conversationId);return id;}
    private MessageView publishedMessage(Long sender,Long other,Long conversationId,Long messageId){MessageView view=jdbc.query("SELECT * FROM curated_messages WHERE id=?",(rs,n)->messageView(sender,map(rs)),messageId).get(0);realtime.afterCommit(other,"CURATED_MESSAGE_CREATED",Map.of("conversationId",conversationId,"messageId",messageId));realtime.afterCommit(sender,"CURATED_MESSAGE_CREATED",Map.of("conversationId",conversationId,"messageId",messageId));return view;}
    private MessageView messageView(Long viewer,Map<String,Object> row){String key=text(row.get("attachment_key"));String url=key==null||key.isBlank()?null:storage.accessUrl(key,StorageVisibility.PRIVATE);return new MessageView(number(row.get("id")),number(row.get("sender_id")),text(row.get("message_type")),text(row.get("content")),url,text(row.get("attachment_name")),number(row.get("attachment_size")),date(row.get("created_at")));}
    private MemberView memberView(Map<String,Object> row){Long id=number(row.get("id"));return new MemberView(id,text(row.get("uid")),displayName(row),text(row.get("avatar_url")),text(row.get("job_title")),integer(row.get("job_years")),id!=null&&realtime.isUserOnline(id));}
    private String displayName(Map<String,Object> row){String nickname=text(row.get("nickname"));return nickname==null||nickname.isBlank()?"UID "+text(row.get("uid")):nickname;}
    private void storeMaterial(Long applicationId,String type,MultipartFile file,boolean identity){if(file==null||file.isEmpty())throw BusinessException.badRequest("认证资料不能为空");if(file.getSize()>500L*1024*1024)throw BusinessException.badRequest("单个认证资料不能超过500MB");FileTypeDetector.DetectedFile detected=fileTypes.detect(file);if(identity&&!"IMAGE".equals(detected.kind()))throw BusinessException.badRequest("实名认证资料仅支持现场拍摄的图片");if(!identity&&!Set.of("IMAGE","VIDEO").contains(detected.kind()))throw BusinessException.badRequest("岗位资料仅支持现场拍摄或录制");StoredFile stored=storage.store(file,"curated-membership/"+applicationId,StorageVisibility.PRIVATE);jdbc.update("INSERT INTO curated_membership_materials(application_id,material_type,media_type,original_name,storage_key,content_type,file_size) VALUES(?,?,?,?,?,?,?)",applicationId,type,detected.kind(),safeName(file.getOriginalFilename()),stored.storageKey(),stored.contentType(),stored.size());}
    private IdentitySnapshot globalIdentity(Long userId){Certification item=certifications.findFirstByUserIdAndCertificationTypeOrderByIdDesc(userId,"IDENTITY").orElse(null);return item==null?new IdentitySnapshot("NOT_APPLIED",null):new IdentitySnapshot(item.getStatus(),item.getRejectionReason());}
    private void requireGlobalIdentityApproved(Long userId){if(!certifications.existsByUserIdAndCertificationTypeAndStatusAndEnabledTrue(userId,"IDENTITY","APPROVED"))throw BusinessException.badRequest("完成实名认证后才可以开通");}
    private List<MultipartFile> validateJobFiles(List<MultipartFile> files) {
        List<MultipartFile> present = files == null
            ? List.of()
            : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (present.isEmpty()) throw BusinessException.badRequest("请至少拍摄一张岗位证明图片或录制一段岗位证明录像");
        int imageCount = 0;
        int videoCount = 0;
        for (MultipartFile file : present) {
            FileTypeDetector.DetectedFile detected = fileTypes.detect(file);
            if ("IMAGE".equals(detected.kind())) {
                imageCount++;
            } else if ("VIDEO".equals(detected.kind())) {
                videoCount++;
                validateJobVideoDuration(file);
            } else {
                throw BusinessException.badRequest("岗位资料仅支持现场拍摄的图片或现场录制的录像");
            }
        }
        if (imageCount > 10) throw BusinessException.badRequest("岗位认证图片最多10张");
        if (videoCount > 1) throw BusinessException.badRequest("岗位认证最多录制一段录像");
        return present;
    }
    private void validateJobVideoDuration(MultipartFile file) {
        try (IsoFile isoFile = new IsoFile(Channels.newChannel(file.getInputStream()))) {
            MovieHeaderBox movieHeader = Path.getPath(isoFile, "moov/mvhd");
            if (movieHeader == null || movieHeader.getTimescale() <= 0) {
                throw BusinessException.badRequest("无法读取岗位认证录像时长，请重新录制");
            }
            double seconds = (double) movieHeader.getDuration() / movieHeader.getTimescale();
            if (seconds > 600.0) throw BusinessException.badRequest("岗位认证录像最长10分钟");
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw BusinessException.badRequest("无法读取岗位认证录像时长，请重新录制");
        }
    }
    private List<MaterialView> materialsFor(String sql,Object...args){return jdbc.query(sql,(rs,n)->new MaterialView(rs.getLong("id"),rs.getString("material_type"),rs.getString("media_type"),rs.getString("original_name"),storage.accessUrl(rs.getString("storage_key"),StorageVisibility.PRIVATE),rs.getLong("file_size"),rs.getTimestamp("created_at").toLocalDateTime()),args);}
    private VoiceCallView voiceCall(Long userId,Long id){Map<String,Object> row=one("SELECT * FROM curated_voice_calls WHERE id=?",id);if(row==null)throw BusinessException.notFound("语音通话不存在");requireCallParticipant(row,userId);return new VoiceCallView(id,number(row.get("conversation_id")),number(row.get("caller_id")),number(row.get("callee_id")),text(row.get("status")),date(row.get("connect_deadline")),date(row.get("answered_at")),date(row.get("connected_at")),date(row.get("ended_at")));}
    private Map<String,Object> lockedCall(Long id){Map<String,Object> row=one("SELECT * FROM curated_voice_calls WHERE id=? FOR UPDATE",id);if(row==null)throw BusinessException.notFound("语音通话不存在");return row;}
    private Long requireCallParticipant(Map<String,Object> call,Long userId){Long caller=number(call.get("caller_id")),callee=number(call.get("callee_id"));if(!Objects.equals(caller,userId)&&!Objects.equals(callee,userId))throw BusinessException.forbidden("无权进入该语音通话");return Objects.equals(caller,userId)?callee:caller;}
    private void requireCallStatus(Map<String,Object> call,String status){if(!status.equals(text(call.get("status"))))throw BusinessException.badRequest("当前语音通话状态不能执行此操作");}
    private void endCallRow(Long id,String status,Long endedBy){jdbc.update("UPDATE curated_voice_calls SET status=?,ended_at=NOW(6),ended_by_user_id=? WHERE id=?",status,endedBy,id);}
    private void publishCallEnded(Map<String,Object> call,String type){Map<String,Object> payload=Map.of("conversationId",number(call.get("conversation_id")),"callId",number(call.get("id")));realtime.afterCommit(number(call.get("caller_id")),type,payload);realtime.afterCommit(number(call.get("callee_id")),type,payload);}
    private Map<String,Object> lockApplication(Long id){Map<String,Object> app=one("SELECT * FROM curated_membership_applications WHERE id=? FOR UPDATE",id);if(app==null)throw BusinessException.notFound("严选直聊申请不存在");return app;}
    private void notifyReview(Long userId,Long applicationId){realtime.afterCommit(userId,"CURATED_APPLICATION_UPDATED",Map.of("applicationId",applicationId));}
    private void audit(AdminUser admin,String action,Object target,String detail,String ip){AdminAuditLog log=new AdminAuditLog();log.setAdminUser(admin);log.setAction(action);log.setTargetType("CURATED_CHAT");log.setTargetId(String.valueOf(target));log.setDetail(detail);log.setIpAddress(ip);audits.save(log);}
    private String requireReason(String value){return requireText(value,2,500,"驳回原因");}
    private String requireText(String value,int min,int max,String label){String v=value==null?"":value.trim();int len=v.codePointCount(0,v.length());if(len<min||len>max)throw BusinessException.badRequest(label+"长度应为"+min+"至"+max+"个字");return v;}
    private void requireFiles(List<MultipartFile> files,int min,int max,String label){int count=files==null?0:(int)files.stream().filter(f->f!=null&&!f.isEmpty()).count();if(count<min||count>max)throw BusinessException.badRequest(label+"需要上传"+min+"至"+max+"个文件");}
    private void requireRequestId(String id){if(id==null||!id.matches("[A-Za-z0-9_-]{12,64}"))throw BusinessException.badRequest("支付请求标识无效");}
    private void ensureMockPayment(){if(!(paymentGateway instanceof MockAlipayGateway))throw BusinessException.forbidden("当前环境未启用模拟支付");}
    private String safeName(String v){if(v==null)return "file";String s=v.replaceAll("[\\r\\n\\t]","_");return s.substring(0,Math.min(s.length(),255));}
    private Map<String,Object> one(String sql,Object...args){List<Map<String,Object>> rows=jdbc.queryForList(sql,args);return rows.isEmpty()?null:rows.get(0);}
    private Map<String,Object> map(java.sql.ResultSet rs)throws java.sql.SQLException{java.sql.ResultSetMetaData md=rs.getMetaData();Map<String,Object> row=new LinkedHashMap<>();for(int i=1;i<=md.getColumnCount();i++)row.put(md.getColumnLabel(i),rs.getObject(i));return row;}
    private String text(Object v){return v==null?null:String.valueOf(v);}
    private Long number(Object v){return v instanceof Number n?n.longValue():v==null?null:Long.valueOf(String.valueOf(v));}
    private Integer integer(Object v){return v instanceof Number n?n.intValue():v==null?null:Integer.valueOf(String.valueOf(v));}
    private LocalDateTime date(Object v){if(v instanceof Timestamp t)return t.toLocalDateTime();if(v instanceof LocalDateTime d)return d;return null;}

    public record MembershipView(boolean applied,String status,String identityStatus,String jobStatus,String identityRejectionReason,String jobRejectionReason,String jobTitle,Integer jobYears,LocalDateTime startedAt,LocalDateTime expiresAt,BigDecimal price,int durationMonths,String priceText){static MembershipView empty(String identityStatus,String identityReason,int durationMonths,BigDecimal price){String overall="REJECTED".equals(identityStatus)?"REJECTED":"NOT_APPLIED";return new MembershipView(false,overall,identityStatus,"NOT_APPLIED",identityReason,null,null,null,null,null,price,durationMonths,membershipPriceText(price,durationMonths));}}
    public record MembershipQuoteView(BigDecimal price,int durationMonths,String priceText,LocalDateTime startsAt,LocalDateTime expiresAt,boolean permanent,String validityText){}
    public record MaterialView(Long id,String materialType,String mediaType,String originalName,String url,Long fileSize,LocalDateTime createdAt){}
    public record PaymentView(Long id,String orderNo,BigDecimal amount,int durationMonths,String priceText,String status,String channel,String paymentPayload,LocalDateTime paidAt,LocalDateTime createdAt){}
    public record MemberView(Long id,String uid,String nickname,String avatarUrl,String jobTitle,Integer jobYears,boolean online){}
    private record IdentitySnapshot(String status,String reason){}
    public record PageView<T>(List<T> content,long totalElements,int page,int size,boolean hasNext){}
    public record MessageView(Long id,Long senderId,String type,String content,String attachmentUrl,String attachmentName,Long attachmentSize,LocalDateTime createdAt){}
    public record ConversationView(Long id,MemberView otherUser,long unreadCount,MessageView lastMessage,List<MessageView> messages,boolean writable,LocalDateTime lastMessageAt){}
    public record VoiceCallView(Long id,Long conversationId,Long callerId,Long calleeId,String status,LocalDateTime connectDeadline,LocalDateTime answeredAt,LocalDateTime connectedAt,LocalDateTime endedAt){}
    public record SignalView(Long id,String type,String payload,LocalDateTime createdAt){}
    public record IceConfig(List<String> urls,String username,String credential){}
}

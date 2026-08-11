package com.shixianwen.admin;

import com.shixianwen.certification.CertificationService;
import com.shixianwen.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class AdminManagementService {
    private final JdbcTemplate jdbc;
    private final CertificationService certifications;
    private final AdminAuditLogRepository audits;

    public Map<String,Object> dashboard() {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("users", count("users","account_status='ACTIVE'"));
        result.put("answerers", count("users","answerer_status='APPROVED' AND account_status='ACTIVE'"));
        result.put("pendingCertifications", count("certifications","status='PENDING'"));
        result.put("activeInquiries", count("inquiries","status IN ('PENDING','ACTIVE','AWAITING_CONFIRMATION','DISPUTED')"));
        result.put("pendingWithdrawals", count("withdrawals","status='PROCESSING'"));
        result.put("openFeedback", count("feedback_records","status='SUBMITTED'"));
        result.put("totalBalance", jdbc.queryForObject("SELECT COALESCE(SUM(available_balance),0) FROM wallet_accounts", java.math.BigDecimal.class));
        result.put("totalFrozen", jdbc.queryForObject("SELECT COALESCE(SUM(frozen_balance),0) FROM wallet_accounts", java.math.BigDecimal.class));
        return result;
    }
    public PageResult users(String keyword,String status,int page,int size) {
        String where=" WHERE (?='' OR u.uid LIKE ? OR u.phone LIKE ? OR COALESCE(u.nickname,'') LIKE ?) AND (?='' OR u.account_status=?) ";
        String q=keyword==null?"":keyword.trim(), s=status==null?"":status.trim(); String like="%"+q+"%";
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM users u"+where,Long.class,q,like,like,like,s,s);
        List<Map<String,Object>> items=jdbc.queryForList("SELECT u.id,u.uid,u.phone,u.nickname,u.avatar_url AS avatarUrl,u.answerer_status AS answererStatus,u.account_status AS accountStatus,u.accepting_inquiries AS acceptingInquiries,u.created_at AS createdAt,COALESCE(w.available_balance,0) AS availableBalance,COALESCE(w.frozen_balance,0) AS frozenBalance FROM users u LEFT JOIN wallet_accounts w ON w.user_id=u.id"+where+" ORDER BY u.id DESC LIMIT ? OFFSET ?",q,like,like,like,s,s,size,page*size);
        return new PageResult(items,total,page,size);
    }
    public PageResult table(String type,String status,int page,int size) {
        TableSpec spec=spec(type); String s=status==null?"":status;
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM "+spec.table+" t WHERE (?='' OR t.status=?)",Long.class,s,s);
        List<Map<String,Object>> rows=jdbc.queryForList(spec.select+" WHERE (?='' OR t.status=?) ORDER BY t.id DESC LIMIT ? OFFSET ?",s,s,size,page*size);
        return new PageResult(rows,total,page,size);
    }
    public List<Map<String,Object>> certificationMaterials(Long id) {
        return jdbc.queryForList("SELECT id,material_kind AS kind,original_name AS name,CONCAT('/uploads/',storage_key) AS url,content_type AS contentType,file_size AS size FROM certification_materials WHERE certification_id=? ORDER BY id",id);
    }
    @Transactional public void reviewCertification(AdminUser admin,Long id,boolean approved,String reason,String ip) {
        if(!approved && (reason==null||reason.isBlank())) throw BusinessException.badRequest("驳回时请填写原因");
        certifications.review(id,approved,reason); audit(admin,"REVIEW_CERTIFICATION","CERTIFICATION",id,String.valueOf(approved),ip);
    }
    @Transactional public void userStatus(AdminUser admin,Long id,String status,String ip) {
        if(!List.of("ACTIVE","SUSPENDED").contains(status)) throw BusinessException.badRequest("用户状态不正确");
        if(jdbc.update("UPDATE users SET account_status=?,accepting_inquiries=CASE WHEN ?='SUSPENDED' THEN FALSE ELSE accepting_inquiries END WHERE id=?",status,status,id)!=1) throw BusinessException.notFound("用户不存在");
        audit(admin,"CHANGE_USER_STATUS","USER",id,status,ip);
    }
    @Transactional public void processWithdrawal(AdminUser admin,Long id,String status,String ip) {
        if(!List.of("COMPLETED","FAILED").contains(status)) throw BusinessException.badRequest("提现状态不正确");
        Map<String,Object> row=jdbc.queryForMap("SELECT user_id,amount,status FROM withdrawals WHERE id=? FOR UPDATE",id);
        if(!"PROCESSING".equals(row.get("status"))) throw BusinessException.badRequest("该提现已经处理");
        jdbc.update("UPDATE withdrawals SET status=?,completed_at=NOW(6) WHERE id=?",status,id);
        if("FAILED".equals(status)) {
            jdbc.update("UPDATE wallet_accounts SET available_balance=available_balance+? WHERE user_id=?",row.get("amount"),row.get("user_id"));
            jdbc.update("INSERT INTO wallet_transactions(user_id,transaction_type,direction,amount,available_after,frozen_after,reference_type,reference_id,description) SELECT ?, 'WITHDRAWAL_REFUND','IN',?,available_balance,frozen_balance,'WITHDRAWAL',?,'提现失败退款' FROM wallet_accounts WHERE user_id=?",row.get("user_id"),row.get("amount"),id,row.get("user_id"));
        }
        audit(admin,"PROCESS_WITHDRAWAL","WITHDRAWAL",id,status,ip);
    }
    @Transactional public void updateRecordStatus(AdminUser admin,String type,Long id,String status,String ip) {
        TableSpec spec=spec(type); if(!List.of("PROCESSING","RESOLVED","CLOSED").contains(status)) throw BusinessException.badRequest("处理状态不正确");
        if(jdbc.update("UPDATE "+spec.table+" SET status=? WHERE id=?",status,id)!=1) throw BusinessException.notFound("记录不存在");
        audit(admin,"UPDATE_"+type.toUpperCase(Locale.ROOT),type.toUpperCase(Locale.ROOT),id,status,ip);
    }
    public PageResult auditLogs(int page,int size) {
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_logs",Long.class);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT l.id,a.display_name AS adminName,l.action,l.target_type AS targetType,l.target_id AS targetId,l.detail,l.ip_address AS ipAddress,l.created_at AS createdAt FROM admin_audit_logs l JOIN admin_users a ON a.id=l.admin_user_id ORDER BY l.id DESC LIMIT ? OFFSET ?",size,page*size);
        return new PageResult(rows,total,page,size);
    }
    public List<Map<String,Object>> customerServiceConversations() {
        return jdbc.queryForList("SELECT u.id AS userId,u.uid,u.nickname,MAX(m.created_at) AS lastMessageAt,SUBSTRING_INDEX(GROUP_CONCAT(m.content ORDER BY m.id DESC SEPARATOR '\\n'),'\\n',1) AS lastMessage,SUM(CASE WHEN m.sender_type='USER' AND m.read_flag=FALSE THEN 1 ELSE 0 END) AS unread FROM customer_service_messages m JOIN users u ON u.id=m.user_id GROUP BY u.id,u.uid,u.nickname ORDER BY lastMessageAt DESC");
    }
    @Transactional public List<Map<String,Object>> customerServiceMessages(Long userId) {
        jdbc.update("UPDATE customer_service_messages SET read_flag=TRUE WHERE user_id=? AND sender_type='USER'",userId);
        return jdbc.queryForList("SELECT id,sender_type AS senderType,content,created_at AS createdAt FROM customer_service_messages WHERE user_id=? ORDER BY id",userId);
    }
    @Transactional public void replyCustomerService(AdminUser admin,Long userId,String content,String ip) {
        if(content==null||content.isBlank()) throw BusinessException.badRequest("回复内容不能为空");
        if(count("users","id="+userId)==0) throw BusinessException.notFound("用户不存在");
        jdbc.update("INSERT INTO customer_service_messages(user_id,sender_type,content,read_flag) VALUES (?,'SERVICE',?,FALSE)",userId,content.trim());
        audit(admin,"REPLY_CUSTOMER_SERVICE","USER",userId,content.trim(),ip);
    }
    private long count(String table,String where){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+where,Long.class);}
    private void audit(AdminUser admin,String action,String type,Object id,String detail,String ip){ AdminAuditLog l=new AdminAuditLog();l.setAdminUser(admin);l.setAction(action);l.setTargetType(type);l.setTargetId(String.valueOf(id));l.setDetail(detail);l.setIpAddress(ip);audits.save(l); }
    private TableSpec spec(String type){return switch(type){
        case "certifications" -> new TableSpec("certifications","SELECT t.id,t.category,t.certification_type AS type,t.title,t.description,t.years,t.status,t.rejection_reason AS rejectionReason,t.submitted_at AS submittedAt,u.uid,u.nickname FROM certifications t JOIN users u ON u.id=t.user_id");
        case "inquiries" -> new TableSpec("inquiries","SELECT t.id,t.topic,t.question,t.amount,t.status,t.funds_status AS fundsStatus,t.created_at AS createdAt,q.uid AS questionerUid,a.uid AS answererUid FROM inquiries t JOIN users q ON q.id=t.questioner_id JOIN users a ON a.id=t.answerer_id");
        case "withdrawals" -> new TableSpec("withdrawals","SELECT t.id,t.amount,t.fee,t.arrival_amount AS arrivalAmount,t.bank_name_snapshot AS bankName,t.card_last_four_snapshot AS lastFour,t.status,t.created_at AS createdAt,u.uid,u.nickname FROM withdrawals t JOIN users u ON u.id=t.user_id");
        case "feedback" -> new TableSpec("feedback_records","SELECT t.id,t.feedback_type AS type,t.category,t.content,t.status,t.created_at AS createdAt,u.uid,u.nickname,tu.uid AS targetUid FROM feedback_records t JOIN users u ON u.id=t.user_id LEFT JOIN users tu ON tu.id=t.target_user_id");
        case "cooperations" -> new TableSpec("business_cooperations","SELECT t.id,t.contact,t.content,t.status,t.created_at AS createdAt,u.uid,u.nickname FROM business_cooperations t JOIN users u ON u.id=t.user_id");
        default -> throw BusinessException.badRequest("管理模块不存在");};}
    private record TableSpec(String table,String select){}
    public record PageResult(List<Map<String,Object>> items,long total,int page,int size){}
}

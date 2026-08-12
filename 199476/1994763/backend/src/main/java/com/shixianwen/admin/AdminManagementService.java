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
    public PageResult table(String type,String status,String category,int page,int size) {
        TableSpec spec=spec(type); String s=status==null?"":status;
        String c=category==null?"":category;
        String categorySql="certifications".equals(type)?" AND (?='' OR t.category=?)":" AND (?='' OR ?='')";
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM "+spec.table+" t WHERE (?='' OR t.status=?)"+categorySql,Long.class,s,s,c,c);
        List<Map<String,Object>> rows=jdbc.queryForList(spec.select+" WHERE (?='' OR t.status=?)"+categorySql+" ORDER BY t.id DESC LIMIT ? OFFSET ?",s,s,c,c,size,page*size);
        return new PageResult(rows,total,page,size);
    }
    public List<Map<String,Object>> certificationMaterials(Long id) {
        return jdbc.queryForList("SELECT id,material_kind AS kind,original_name AS name,CONCAT('/uploads/',storage_key) AS url,content_type AS contentType,file_size AS size FROM certification_materials WHERE certification_id=? ORDER BY id",id);
    }
    @Transactional public void reviewCertification(AdminUser admin,Long id,boolean approved,String reason,Long jobId,Integer years,String ip) {
        if(!approved && (reason==null||reason.isBlank())) throw BusinessException.badRequest("驳回时请填写原因");
        Map<String,Object> certification=jdbc.queryForMap("SELECT user_id AS userId,certification_type AS type,title,status FROM certifications WHERE id=? FOR UPDATE",id);
        if(!"PENDING".equals(certification.get("status"))) throw BusinessException.badRequest("该认证已经处理");
        if(approved&&"MAIN_JOB".equals(certification.get("type"))){
            if(jobId==null) throw BusinessException.badRequest("请选择审核判定的岗位");
            List<String> jobNames=jdbc.query(
                "SELECT name FROM jobs WHERE id=? AND active=TRUE",
                (resultSet,rowNumber)->resultSet.getString("name"),
                jobId
            );
            if(jobNames.isEmpty()) throw BusinessException.badRequest("所选岗位不存在或已停用");
            if(years==null) throw BusinessException.badRequest("请填写工龄");
            if(years<1||years>80) throw BusinessException.badRequest("工龄必须是1至80之间的整数");
            String selectedJobName=jobNames.get(0);
            jdbc.update("UPDATE certifications SET title=?,years=? WHERE id=?",selectedJobName,years,id);
            certification.put("title",selectedJobName);
        }
        certifications.review(id,approved,reason);
        if("MAIN_JOB".equals(certification.get("type"))){
            if(approved){
                jdbc.update("DELETE FROM user_jobs WHERE user_id=?",certification.get("userId"));
                jdbc.update("INSERT INTO user_jobs(user_id,job_id,certification_id,verified) VALUES (?,?,?,TRUE)",certification.get("userId"),jobId,id);
            }else {
                jdbc.update("DELETE FROM user_jobs WHERE certification_id=?",id);
                jdbc.update("UPDATE users SET answerer_status='PENDING',accepting_inquiries=FALSE WHERE id=?",certification.get("userId"));
            }
        }
        audit(admin,"REVIEW_CERTIFICATION","CERTIFICATION",id,String.valueOf(approved),ip);
    }
    @Transactional public void userStatus(AdminUser admin,Long id,String status,String ip) {
        if(!List.of("ACTIVE","SUSPENDED").contains(status)) throw BusinessException.badRequest("用户状态不正确");
        if(jdbc.update("UPDATE users SET account_status=?,accepting_inquiries=CASE WHEN ?='SUSPENDED' THEN FALSE ELSE accepting_inquiries END WHERE id=?",status,status,id)!=1) throw BusinessException.notFound("用户不存在");
        audit(admin,"CHANGE_USER_STATUS","USER",id,status,ip);
    }
    public List<Map<String,Object>> jobs(){return jdbc.queryForList("SELECT j.id,j.name,j.description,j.active,j.created_at AS createdAt,COUNT(DISTINCT uj.user_id) AS userCount,COUNT(DISTINCT mj.matter_id) AS matterCount FROM jobs j LEFT JOIN user_jobs uj ON uj.job_id=j.id LEFT JOIN discovery_matter_jobs mj ON mj.job_id=j.id GROUP BY j.id,j.name,j.description,j.active,j.created_at ORDER BY j.active DESC,j.name,j.id");}
    public List<Map<String,Object>> jobUsers(Long jobId){
        if(count("jobs","id="+jobId)==0)throw BusinessException.notFound("岗位不存在");
        return jdbc.queryForList(
            "SELECT u.id AS userId,u.uid,u.nickname,u.avatar_url AS avatarUrl,"+
                "u.account_status AS accountStatus,uj.verified,c.years "+
                "FROM user_jobs uj JOIN users u ON u.id=uj.user_id " +
                "LEFT JOIN certifications c ON c.id=uj.certification_id " +
                "WHERE uj.job_id=? ORDER BY uj.verified DESC,u.id DESC",
            jobId
        );
    }
    @Transactional public void createJob(AdminUser admin,String name,String description,String ip){String value=required(name,"岗位名称不能为空");ensureJobName(value,null);jdbc.update("INSERT INTO jobs(name,description,active) VALUES (?,?,TRUE)",value,cleanDescription(description));Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);audit(admin,"CREATE_JOB","JOB",id,value,ip);}
    @Transactional public void updateJob(AdminUser admin,Long id,String name,String description,Boolean active,String ip){String value=required(name,"岗位名称不能为空");ensureJobName(value,id);if(jdbc.update("UPDATE jobs SET name=?,description=?,active=? WHERE id=?",value,cleanDescription(description),active==null||active,id)!=1)throw BusinessException.notFound("岗位不存在");audit(admin,"UPDATE_JOB","JOB",id,value,ip);}
    @Transactional public void deleteJob(AdminUser admin,Long id,String ip){if(count("user_jobs","job_id="+id)>0||count("discovery_matter_jobs","job_id="+id)>0)throw BusinessException.badRequest("该岗位仍关联用户或事情，请先解除关联");if(jdbc.update("DELETE FROM jobs WHERE id=?",id)!=1)throw BusinessException.notFound("岗位不存在");audit(admin,"DELETE_JOB","JOB",id,"DELETE",ip);}
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
    public Map<String,Object> discovery() {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("categories",jdbc.queryForList("SELECT id,main_category AS mainCategory,name,sort_order AS sortOrder,active FROM discovery_categories ORDER BY FIELD(main_category,'LIFE','WORK','ENTERTAINMENT'),sort_order,id"));
        result.put("matters",jdbc.queryForList("SELECT m.id,m.category_id AS categoryId,m.title,m.sort_order AS sortOrder,m.active,c.main_category AS mainCategory,c.name AS categoryName FROM discovery_matters m JOIN discovery_categories c ON c.id=m.category_id ORDER BY FIELD(c.main_category,'LIFE','WORK','ENTERTAINMENT'),c.sort_order,m.sort_order,m.id"));
        result.put("matterJobs",jdbc.queryForList("SELECT mj.matter_id AS matterId,mj.job_id AS jobId,mj.participation_type AS type,mj.sort_order AS sortOrder,j.name AS jobName FROM discovery_matter_jobs mj JOIN jobs j ON j.id=mj.job_id ORDER BY mj.matter_id,CASE mj.participation_type WHEN 'PRIMARY' THEN 0 ELSE 1 END,mj.sort_order,j.name"));
        result.put("jobs",jobs());
        result.put("experiences",jdbc.queryForList("SELECT t.id,t.title,t.description,t.status,t.discovery_category_id AS categoryId,t.reviewed_at AS reviewedAt,u.uid,u.nickname,c.main_category AS mainCategory,c.name AS categoryName FROM certifications t JOIN users u ON u.id=t.user_id LEFT JOIN discovery_categories c ON c.id=t.discovery_category_id WHERE t.category='EXPERIENCE' AND t.status='APPROVED' ORDER BY CASE WHEN t.discovery_category_id IS NULL THEN 0 ELSE 1 END,t.id DESC"));
        return result;
    }
    @Transactional public void createCategory(AdminUser admin,String mainCategory,String name,Integer sortOrder,String ip) {
        String main=mainCategory(mainCategory), value=required(name,"子分类名称不能为空");
        ensureCategoryNameAvailable(main,value,null);
        int tail=jdbc.queryForObject("SELECT COUNT(*)+1 FROM discovery_categories WHERE main_category=?",Integer.class,main);
        jdbc.update("INSERT INTO discovery_categories(main_category,name,sort_order,active) VALUES (?,?,?,TRUE)",main,value,tail);
        Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        reorderCategories(main,id,sortOrder==null?tail:sortOrder);
        audit(admin,"CREATE_DISCOVERY_CATEGORY","DISCOVERY_CATEGORY",id,main+"/"+value,ip);
    }
    @Transactional public void updateCategory(AdminUser admin,Long id,String mainCategory,String name,Integer sortOrder,Boolean active,String ip) {
        String main=mainCategory(mainCategory), value=required(name,"子分类名称不能为空");
        ensureCategoryNameAvailable(main,value,id);
        String oldMain=jdbc.query("SELECT main_category FROM discovery_categories WHERE id=?",rs->rs.next()?rs.getString(1):null,id);
        if(oldMain==null) throw BusinessException.notFound("子分类不存在");
        jdbc.update("UPDATE discovery_categories SET main_category=?,name=?,active=? WHERE id=?",main,value,active==null||active,id);
        if(!oldMain.equals(main)) normalizeCategories(oldMain);
        reorderCategories(main,id,sortOrder==null?Integer.MAX_VALUE:sortOrder);
        audit(admin,"UPDATE_DISCOVERY_CATEGORY","DISCOVERY_CATEGORY",id,main+"/"+value,ip);
    }
    @Transactional public void deleteCategory(AdminUser admin,Long id,String ip) {
        if(count("discovery_matters","category_id="+id)>0) throw BusinessException.badRequest("该分类下仍有事情，请先删除或转移事情");
        if(count("certifications","discovery_category_id="+id)>0) throw BusinessException.badRequest("该分类仍关联认证经历，请先取消或调整经历分类");
        String main=jdbc.query("SELECT main_category FROM discovery_categories WHERE id=?",rs->rs.next()?rs.getString(1):null,id);
        if(main==null||jdbc.update("DELETE FROM discovery_categories WHERE id=?",id)!=1) throw BusinessException.notFound("子分类不存在");
        normalizeCategories(main);
        audit(admin,"DELETE_DISCOVERY_CATEGORY","DISCOVERY_CATEGORY",id,"DELETE",ip);
    }
    @Transactional public void createMatter(AdminUser admin,Long categoryId,String title,Integer sortOrder,List<AdminManagementController.MatterJobRequest> jobs,String ip) {
        requireCategory(categoryId); String value=required(title,"事情名称不能为空");
        ensureMatterTitleAvailable(categoryId,value,null);
        int tail=jdbc.queryForObject("SELECT COUNT(*)+1 FROM discovery_matters WHERE category_id=?",Integer.class,categoryId);
        jdbc.update("INSERT INTO discovery_matters(category_id,title,sort_order,active) VALUES (?,?,?,TRUE)",categoryId,value,tail);
        Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        replaceMatterJobs(id,jobs);
        reorderMatters(categoryId,id,sortOrder==null?tail:sortOrder);
        audit(admin,"CREATE_DISCOVERY_MATTER","DISCOVERY_MATTER",id,value,ip);
    }
    @Transactional public void updateMatter(AdminUser admin,Long id,Long categoryId,String title,Integer sortOrder,Boolean active,List<AdminManagementController.MatterJobRequest> jobs,String ip) {
        requireCategory(categoryId); String value=required(title,"事情名称不能为空");
        ensureMatterTitleAvailable(categoryId,value,id);
        Long oldCategory=jdbc.query("SELECT category_id FROM discovery_matters WHERE id=?",rs->rs.next()?rs.getLong(1):null,id);
        if(oldCategory==null) throw BusinessException.notFound("事情不存在");
        jdbc.update("UPDATE discovery_matters SET category_id=?,title=?,active=? WHERE id=?",categoryId,value,active==null||active,id);
        replaceMatterJobs(id,jobs);
        if(!oldCategory.equals(categoryId)) normalizeMatters(oldCategory);
        reorderMatters(categoryId,id,sortOrder==null?Integer.MAX_VALUE:sortOrder);
        audit(admin,"UPDATE_DISCOVERY_MATTER","DISCOVERY_MATTER",id,value,ip);
    }
    @Transactional public void deleteMatter(AdminUser admin,Long id,String ip) {
        Long categoryId=jdbc.query("SELECT category_id FROM discovery_matters WHERE id=?",rs->rs.next()?rs.getLong(1):null,id);
        if(categoryId==null||jdbc.update("DELETE FROM discovery_matters WHERE id=?",id)!=1) throw BusinessException.notFound("事情不存在");
        normalizeMatters(categoryId);
        audit(admin,"DELETE_DISCOVERY_MATTER","DISCOVERY_MATTER",id,"DELETE",ip);
    }
    @Transactional public void classifyExperience(AdminUser admin,Long id,Long categoryId,String ip) {
        if(categoryId!=null) requireCategory(categoryId);
        if(jdbc.update("UPDATE certifications SET discovery_category_id=? WHERE id=? AND category='EXPERIENCE' AND status='APPROVED'",categoryId,id)!=1) throw BusinessException.badRequest("仅可分类已审核通过的亲身经历");
        audit(admin,"CLASSIFY_EXPERIENCE","CERTIFICATION",id,String.valueOf(categoryId),ip);
    }
    private void requireCategory(Long id){if(id==null||count("discovery_categories","id="+id+" AND active=TRUE")==0) throw BusinessException.badRequest("请选择有效的子分类");}
    private void ensureCategoryNameAvailable(String main,String name,Long excludedId){
        Long total=excludedId==null
            ? jdbc.queryForObject("SELECT COUNT(*) FROM discovery_categories WHERE main_category=? AND name=?",Long.class,main,name)
            : jdbc.queryForObject("SELECT COUNT(*) FROM discovery_categories WHERE main_category=? AND name=? AND id<>?",Long.class,main,name,excludedId);
        if(total!=null&&total>0) throw BusinessException.badRequest("该业务大类下已存在同名子分类");
    }
    private void ensureMatterTitleAvailable(Long categoryId,String title,Long excludedId){
        Long total=excludedId==null
            ? jdbc.queryForObject("SELECT COUNT(*) FROM discovery_matters WHERE category_id=? AND title=?",Long.class,categoryId,title)
            : jdbc.queryForObject("SELECT COUNT(*) FROM discovery_matters WHERE category_id=? AND title=? AND id<>?",Long.class,categoryId,title,excludedId);
        if(total!=null&&total>0) throw BusinessException.badRequest("该子分类下已存在同名事情");
    }
    private void replaceMatterJobs(Long matterId,List<AdminManagementController.MatterJobRequest> jobs){
        jdbc.update("DELETE FROM discovery_matter_jobs WHERE matter_id=?",matterId);
        if(jobs==null||jobs.isEmpty()) throw BusinessException.badRequest("每件事情至少设置一个重点问的岗位");
        int primaryOrder=1, supportingOrder=1;
        Set<Long> seen=new HashSet<>();
        for(AdminManagementController.MatterJobRequest job:jobs){
            if(job==null||job.jobId()==null) continue;
            if(!seen.add(job.jobId())) throw BusinessException.badRequest("同一岗位不能重复配置");
            String type="SUPPORTING".equalsIgnoreCase(job.type())?"SUPPORTING":"PRIMARY";
            if(count("jobs","id="+job.jobId()+" AND active=TRUE")==0) throw BusinessException.badRequest("所选岗位不存在或已停用");
            int order="PRIMARY".equals(type)?primaryOrder++:supportingOrder++;
            jdbc.update("INSERT INTO discovery_matter_jobs(matter_id,job_id,participation_type,sort_order) VALUES (?,?,?,?)",matterId,job.jobId(),type,order);
        }
        if(primaryOrder==1) throw BusinessException.badRequest("每件事情至少设置一个重点问的岗位");
    }
    private void ensureJobName(String name,Long excludedId){Long total=excludedId==null?jdbc.queryForObject("SELECT COUNT(*) FROM jobs WHERE name=?",Long.class,name):jdbc.queryForObject("SELECT COUNT(*) FROM jobs WHERE name=? AND id<>?",Long.class,name,excludedId);if(total!=null&&total>0)throw BusinessException.badRequest("岗位名称已存在");}
    private String cleanDescription(String value){if(value==null||value.isBlank())return null;String result=value.trim();if(result.length()>240)throw BusinessException.badRequest("介绍不能超过240个字");return result;}
    private String mainCategory(String value){String result=value==null?"":value.trim().toUpperCase(Locale.ROOT);if(!List.of("LIFE","WORK","ENTERTAINMENT").contains(result)) throw BusinessException.badRequest("主分类不正确");return result;}
    private String required(String value,String message){if(value==null||value.isBlank()) throw BusinessException.badRequest(message);return value.trim();}
    private void reorderCategories(String main,Long movingId,int requestedPosition){
        List<Long> ids=jdbc.queryForList("SELECT id FROM discovery_categories WHERE main_category=? AND id<>? ORDER BY sort_order,id",Long.class,main,movingId);
        ids.add(Math.max(0,Math.min(requestedPosition-1,ids.size())),movingId);
        for(int index=0;index<ids.size();index++) jdbc.update("UPDATE discovery_categories SET sort_order=? WHERE id=?",index+1,ids.get(index));
    }
    private void normalizeCategories(String main){List<Long> ids=jdbc.queryForList("SELECT id FROM discovery_categories WHERE main_category=? ORDER BY sort_order,id",Long.class,main);for(int i=0;i<ids.size();i++) jdbc.update("UPDATE discovery_categories SET sort_order=? WHERE id=?",i+1,ids.get(i));}
    private void reorderMatters(Long categoryId,Long movingId,int requestedPosition){
        List<Long> ids=jdbc.queryForList("SELECT id FROM discovery_matters WHERE category_id=? AND id<>? ORDER BY sort_order,id",Long.class,categoryId,movingId);
        ids.add(Math.max(0,Math.min(requestedPosition-1,ids.size())),movingId);
        for(int index=0;index<ids.size();index++) jdbc.update("UPDATE discovery_matters SET sort_order=? WHERE id=?",index+1,ids.get(index));
    }
    private void normalizeMatters(Long categoryId){List<Long> ids=jdbc.queryForList("SELECT id FROM discovery_matters WHERE category_id=? ORDER BY sort_order,id",Long.class,categoryId);for(int i=0;i<ids.size();i++) jdbc.update("UPDATE discovery_matters SET sort_order=? WHERE id=?",i+1,ids.get(i));}
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

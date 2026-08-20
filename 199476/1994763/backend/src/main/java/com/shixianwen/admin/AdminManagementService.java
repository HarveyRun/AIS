package com.shixianwen.admin;

import com.shixianwen.certification.CertificationService;
import com.shixianwen.common.BusinessException;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.wallet.PlatformServiceFeePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import com.shixianwen.realtime.RealtimePublisher;

@Service @RequiredArgsConstructor
public class AdminManagementService {
    private final JdbcTemplate jdbc;
    private final CertificationService certifications;
    private final AdminAuditLogRepository audits;
    private final RealtimePublisher realtime;
    private final SensitiveWordService sensitiveWords;
    private final FileStorage fileStorage;
    private final SecurityEventService securityEvents;
    private final PlatformServiceFeePolicy platformServiceFeePolicy;

    public Map<String,Object> dashboard() {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("users", count("users","account_status='ACTIVE' AND account_type='NORMAL'"));
        result.put("answerers", jdbc.queryForObject(
            "SELECT COUNT(*) FROM users u WHERE u.account_status='ACTIVE' AND u.account_type='NORMAL' " +
                "AND EXISTS (SELECT 1 FROM certifications ci WHERE ci.user_id=u.id AND ci.certification_type='IDENTITY' AND ci.status='APPROVED' AND ci.enabled=TRUE AND ci.deleted_at IS NULL) " +
                "AND EXISTS (SELECT 1 FROM certifications cj WHERE cj.user_id=u.id AND cj.certification_type='MAIN_JOB' AND cj.status='APPROVED' AND cj.enabled=TRUE AND cj.deleted_at IS NULL)",
            Long.class
        ));
        result.put("pendingCertifications", jdbc.queryForObject(
            "SELECT COUNT(*) FROM certifications c JOIN users u ON u.id=c.user_id WHERE c.status='PENDING' AND c.deleted_at IS NULL AND u.account_type='NORMAL'",
            Long.class
        ));
        result.put("activeInquiries", jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiries i JOIN users u ON u.id=i.questioner_id WHERE i.status IN ('PENDING','ACTIVE','AWAITING_CONFIRMATION','DISPUTED') AND u.account_type='NORMAL'",
            Long.class
        ));
        result.put("pendingWithdrawals", jdbc.queryForObject(
            "SELECT COUNT(*) FROM withdrawals w JOIN users u ON u.id=w.user_id WHERE w.status='PROCESSING' AND u.account_type='NORMAL'",
            Long.class
        ));
        result.put("openFeedback", jdbc.queryForObject(
            "SELECT COUNT(*) FROM feedback_records f JOIN users u ON u.id=f.user_id WHERE f.status='SUBMITTED' AND u.account_type='NORMAL'",
            Long.class
        ));
        result.put("totalBalance", jdbc.queryForObject(
            "SELECT COALESCE(SUM(w.available_balance),0) FROM wallet_accounts w JOIN users u ON u.id=w.user_id WHERE u.account_type='NORMAL'",
            java.math.BigDecimal.class
        ));
        result.put("totalFrozen", jdbc.queryForObject(
            "SELECT COALESCE(SUM(w.frozen_balance),0) FROM wallet_accounts w JOIN users u ON u.id=w.user_id WHERE u.account_type='NORMAL'",
            java.math.BigDecimal.class
        ));
        return result;
    }

    public Map<String, Object> platformFeeSetting() {
        Map<String, Object> result = new LinkedHashMap<>();
        jdbc.queryForList(
            "SELECT client_platform AS clientPlatform,service_fee_rate * 100 AS ratePercent," +
                "updated_at AS updatedAt FROM platform_fee_settings WHERE client_platform IN ('ANDROID','IOS')"
        ).forEach(row -> {
            String prefix = "IOS".equals(row.get("clientPlatform")) ? "ios" : "android";
            result.put(prefix + "RatePercent", row.get("ratePercent"));
            result.put(prefix + "UpdatedAt", row.get("updatedAt"));
        });
        if (!result.containsKey("androidRatePercent") || !result.containsKey("iosRatePercent")) {
            throw BusinessException.badRequest("Android或iOS平台服务费配置不存在");
        }
        return result;
    }

    @Transactional
    public Map<String, Object> updatePlatformFee(
        AdminUser admin,
        BigDecimal androidRatePercent,
        BigDecimal iosRatePercent,
        String ip
    ) {
        BigDecimal androidRate = rateFromPercent(androidRatePercent, "Android");
        BigDecimal iosRate = rateFromPercent(iosRatePercent, "iOS");
        updatePlatformRate("ANDROID", androidRate, admin.getId());
        updatePlatformRate("IOS", iosRate, admin.getId());
        audit(
            admin, "UPDATE_PLATFORM_SERVICE_FEE", "PLATFORM_FEE_SETTING", "ANDROID_IOS",
            "Android=" + androidRatePercent + "%, iOS=" + iosRatePercent + "%", ip
        );
        return platformFeeSetting();
    }

    private BigDecimal rateFromPercent(BigDecimal ratePercent, String platformName) {
        if (ratePercent == null) throw BusinessException.badRequest("请输入" + platformName + "平台服务费率");
        if (ratePercent.compareTo(BigDecimal.ZERO) < 0
            || ratePercent.compareTo(new BigDecimal("99")) > 0) {
            throw BusinessException.badRequest(platformName + "平台服务费率必须在0%至99%之间");
        }
        return platformServiceFeePolicy.requireValidRate(
            ratePercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
        );
    }

    private void updatePlatformRate(String platform, BigDecimal rate, Long adminId) {
        int changed = jdbc.update(
            "UPDATE platform_fee_settings SET service_fee_rate=?,updated_by_admin_id=? WHERE client_platform=?",
            rate, adminId, platform
        );
        if (changed != 1) throw BusinessException.badRequest(platform + "平台服务费配置不存在");
    }
    public PageResult users(String keyword,String status,int page,int size) {
        jdbc.update("UPDATE users SET account_status='ACTIVE',ban_reason=NULL,banned_at=NULL,ban_until=NULL,banned_by_admin_id=NULL WHERE account_status='SUSPENDED' AND ban_until IS NOT NULL AND ban_until<=NOW(6)");
        String where=" WHERE (?='' OR u.uid LIKE ? OR u.phone LIKE ? OR COALESCE(u.nickname,'') LIKE ?) AND (?='' OR u.account_status=?) ";
        String q=keyword==null?"":keyword.trim(), s=status==null?"":status.trim(); String like="%"+q+"%";
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM users u"+where,Long.class,q,like,like,like,s,s);
        List<Map<String,Object>> items=jdbc.queryForList("SELECT u.id,u.uid,u.phone,u.nickname,u.avatar_url AS avatarUrl,u.account_type AS accountType,u.answerer_status AS answererStatus,u.account_status AS accountStatus,u.accepting_inquiries AS acceptingInquiries,u.ban_reason AS banReason,u.banned_at AS bannedAt,u.ban_until AS banUntil,u.created_at AS createdAt,COALESCE(w.available_balance,0) AS availableBalance,COALESCE(w.frozen_balance,0) AS frozenBalance FROM users u LEFT JOIN wallet_accounts w ON w.user_id=u.id"+where+" ORDER BY u.id DESC LIMIT ? OFFSET ?",q,like,like,like,s,s,size,page*size);
        return new PageResult(items,total,page,size);
    }
    public PageResult table(String type,String status,String category,String keyword,int page,int size) {
        TableSpec spec = spec(type);
        String normalizedStatus = status == null ? "" : status.trim();
        String normalizedCategory = category == null ? "" : category.trim();
        String normalizedKeyword = keyword == null ? "" : keyword.trim();

        StringBuilder where = new StringBuilder(" WHERE (?='' OR t.status=?)");
        List<Object> parameters = new ArrayList<>();
        parameters.add(normalizedStatus);
        parameters.add(normalizedStatus);

        if ("certifications".equals(type)) {
            where.append(" AND (?='' OR t.category=?) AND t.deleted_at IS NULL");
            parameters.add(normalizedCategory);
            parameters.add(normalizedCategory);
        }

        if (!normalizedKeyword.isBlank() && !spec.userAliases().isEmpty()) {
            String likeKeyword = "%" + normalizedKeyword + "%";
            List<String> userConditions = new ArrayList<>();
            for (String alias : spec.userAliases()) {
                userConditions.add(
                    "(" + alias + ".uid LIKE ? OR " + alias + ".phone LIKE ? OR CAST(" + alias + ".id AS CHAR) LIKE ?)"
                );
                parameters.add(likeKeyword);
                parameters.add(likeKeyword);
                parameters.add(likeKeyword);
            }
            where.append(" AND (").append(String.join(" OR ", userConditions)).append(")");
        }

        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + spec.from() + where,
            Long.class,
            parameters.toArray()
        );
        List<Object> rowParameters = new ArrayList<>(parameters);
        rowParameters.add(size);
        rowParameters.add(page * size);
        List<Map<String,Object>> rows = jdbc.queryForList(
            spec.select() + where + " ORDER BY t.id DESC LIMIT ? OFFSET ?",
            rowParameters.toArray()
        );
        return new PageResult(rows,total,page,size);
    }
    public List<Map<String,Object>> certificationMaterials(Long id) {
        return jdbc.queryForList(
            "SELECT id,material_kind AS kind,original_name AS name,storage_key AS storageKey,public_url AS publicUrl,content_type AS contentType,file_size AS size FROM certification_materials WHERE certification_id=? AND deleted_at IS NULL ORDER BY id",
            id
        ).stream().map(this::withPrivateMaterialUrl).toList();
    }

    private Map<String,Object> withPrivateMaterialUrl(Map<String,Object> source) {
        Map<String,Object> item = new LinkedHashMap<>(source);
        String legacyUrl = Objects.toString(item.remove("publicUrl"), "");
        String storageKey = Objects.toString(item.remove("storageKey"), "");
        item.put(
            "url",
            legacyUrl.isBlank()
                ? fileStorage.accessUrl(storageKey, StorageVisibility.PRIVATE)
                : legacyUrl
        );
        return item;
    }
    @Transactional public void reviewCertification(AdminUser admin,Long id,boolean approved,String reason,Long jobId,Integer years,Long experienceId,String ip) {
        if(!approved && (reason==null||reason.isBlank())) throw BusinessException.badRequest("驳回时请填写原因");
        Map<String,Object> certification=jdbc.queryForMap("SELECT user_id AS userId,certification_type AS type,title,status FROM certifications WHERE id=? AND deleted_at IS NULL FOR UPDATE",id);
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
        if(approved&&"EXPERIENCE".equals(certification.get("type"))){
            requireExperience(experienceId);
            jdbc.update(
                "UPDATE certifications c JOIN discovery_experiences e ON e.id=? SET c.discovery_experience_id=e.id,c.discovery_category_id=e.category_id WHERE c.id=?",
                experienceId,id
            );
        }
        certifications.review(id,approved,reason);
        if("MAIN_JOB".equals(certification.get("type"))){
            if(approved){
                jdbc.update("INSERT INTO user_jobs(user_id,job_id,certification_id,verified,deleted_at) VALUES (?,?,?,TRUE,NULL) ON DUPLICATE KEY UPDATE job_id=VALUES(job_id),certification_id=VALUES(certification_id),verified=TRUE,deleted_at=NULL",certification.get("userId"),jobId,id);
            }else {
                jdbc.update("UPDATE user_jobs SET verified=FALSE,deleted_at=NOW(6) WHERE certification_id=?",id);
                jdbc.update("UPDATE users SET answerer_status='PENDING',accepting_inquiries=FALSE WHERE id=?",certification.get("userId"));
            }
        }
        audit(admin,"REVIEW_CERTIFICATION","CERTIFICATION",id,String.valueOf(approved),ip);
    }
    @Transactional public void setCertificationEnabled(AdminUser admin,Long id,boolean enabled,String ip){
        Map<String,Object> item=certificationForUpdate(id);
        if(!"APPROVED".equals(item.get("status"))) throw BusinessException.badRequest("仅可调整已通过的认证");
        jdbc.update("UPDATE certifications SET enabled=? WHERE id=?",enabled,id);
        if("MAIN_JOB".equals(item.get("type"))) jdbc.update("UPDATE user_jobs SET verified=?,deleted_at=CASE WHEN ?=TRUE THEN NULL ELSE deleted_at END WHERE certification_id=?",enabled,enabled,id);
        if (!"EXPERIENCE".equals(item.get("type"))) {
            // 恢复认证只恢复答主资格，不替用户打开“接受新询问”。
            syncAnswererStatus(((Number)item.get("userId")).longValue(),false);
        }
        audit(admin,"CHANGE_CERTIFICATION_ENABLED","CERTIFICATION",id,String.valueOf(enabled),ip);
    }
    @Transactional public void deleteCertification(AdminUser admin,Long id,String ip){
        Map<String,Object> item=certificationForUpdate(id);
        jdbc.update("UPDATE certifications SET enabled=FALSE,deleted_at=NOW(6) WHERE id=?",id);
        if("MAIN_JOB".equals(item.get("type"))) jdbc.update("UPDATE user_jobs SET verified=FALSE,deleted_at=NOW(6) WHERE certification_id=?",id);
        syncAnswererStatus(((Number)item.get("userId")).longValue(),false);
        audit(admin,"DELETE_CERTIFICATION","CERTIFICATION",id,"SOFT_DELETE",ip);
    }
    @Transactional public void editCertification(AdminUser admin,Long id,String title,String description,Long jobId,Integer years,String ip){
        Map<String,Object> item=certificationForUpdate(id); String type=String.valueOf(item.get("type"));
        if(!List.of("IDENTITY","MAIN_JOB").contains(type)) throw BusinessException.badRequest("亲身经历认证暂不支持编辑");
        if("MAIN_JOB".equals(type)){
            if(jobId==null) throw BusinessException.badRequest("请选择岗位");
            String jobName=jdbc.query("SELECT name FROM jobs WHERE id=? AND active=TRUE AND deleted_at IS NULL",rs->rs.next()?rs.getString(1):null,jobId);
            if(jobName==null) throw BusinessException.badRequest("所选岗位不存在或已停用");
            if(years==null||years<1||years>80) throw BusinessException.badRequest("工龄必须是1至80之间的整数");
            jdbc.update("UPDATE certifications SET title=?,description=?,years=? WHERE id=?",jobName,cleanDescription(description),years,id);
            if("APPROVED".equals(item.get("status"))){
                jdbc.update("UPDATE user_jobs SET job_id=? WHERE certification_id=?",jobId,id);
            }
        }else{
            jdbc.update("UPDATE certifications SET title=?,description=? WHERE id=?",required(title,"认证名称不能为空"),cleanDescription(description),id);
        }
        audit(admin,"EDIT_CERTIFICATION","CERTIFICATION",id,type,ip);
    }
    private Map<String,Object> certificationForUpdate(Long id){
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT user_id AS userId,certification_type AS type,status,enabled FROM certifications WHERE id=? AND deleted_at IS NULL FOR UPDATE",id);
        if(rows.isEmpty()) throw BusinessException.notFound("认证不存在"); return rows.get(0);
    }
    private void syncAnswererStatus(Long userId,boolean allowEnable){
        Long identity=jdbc.queryForObject("SELECT COUNT(*) FROM certifications WHERE user_id=? AND certification_type='IDENTITY' AND status='APPROVED' AND enabled=TRUE AND deleted_at IS NULL",Long.class,userId);
        Long job=jdbc.queryForObject("SELECT COUNT(*) FROM certifications WHERE user_id=? AND certification_type='MAIN_JOB' AND status='APPROVED' AND enabled=TRUE AND deleted_at IS NULL",Long.class,userId);
        boolean approved=identity!=null&&identity>0&&job!=null&&job>0;
        jdbc.update("UPDATE users SET answerer_status=?,accepting_inquiries=CASE WHEN ?=TRUE AND account_status='ACTIVE' AND ?=TRUE THEN TRUE WHEN ?=FALSE THEN FALSE ELSE accepting_inquiries END WHERE id=?",approved?"APPROVED":"PENDING",approved,allowEnable,approved,userId);
    }
    @Transactional
    public void userStatus(
        AdminUser admin,
        Long id,
        String status,
        String duration,
        String reason,
        String ip
    ) {
        if (!List.of("ACTIVE", "SUSPENDED").contains(status)) {
            throw BusinessException.badRequest("用户状态不正确");
        }
        if ("ACTIVE".equals(status)) {
            int affected = jdbc.update(
                "UPDATE users SET account_status='ACTIVE',ban_reason=NULL," +
                    "banned_at=NULL,ban_until=NULL,banned_by_admin_id=NULL WHERE id=?",
                id
            );
            if (affected != 1) throw BusinessException.notFound("用户不存在");
            audit(admin, "RESTORE_USER", "USER", id, "解除封禁", ip);
            return;
        }

        String cleanReason = required(reason, "请填写处罚原因");
        if (cleanReason.length() > 300) {
            throw BusinessException.badRequest("处罚原因最多300个字");
        }
        String cleanDuration = duration == null
            ? ""
            : duration.trim().toUpperCase(Locale.ROOT);
        LocalDateTime banUntil = switch (cleanDuration) {
            case "DAYS_3" -> LocalDateTime.now().plusDays(3);
            case "DAYS_7" -> LocalDateTime.now().plusDays(7);
            case "DAYS_15" -> LocalDateTime.now().plusDays(15);
            case "PERMANENT" -> null;
            default -> throw BusinessException.badRequest("请选择封禁时长");
        };
        int affected = jdbc.update(
            "UPDATE users SET account_status='SUSPENDED'," +
                "ban_reason=?,banned_at=NOW(6)," +
                "ban_until=?,banned_by_admin_id=? WHERE id=?",
            cleanReason,
            banUntil,
            admin.getId(),
            id
        );
        if (affected != 1) throw BusinessException.notFound("用户不存在");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", cleanReason);
        payload.put("duration", cleanDuration);
        payload.put("banUntil", banUntil);
        payload.put("permanent", banUntil == null);
        realtime.afterCommit(id, "ACCOUNT_PENALTY", payload);
        audit(
            admin,
            "PENALIZE_USER",
            "USER",
            id,
            cleanDuration + " | " + cleanReason,
            ip
        );
    }
    public PageResult jobs(String jobName,int page,int size){
        String keyword=jobName==null?"":jobName.trim();
        String like="%"+keyword+"%";
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM jobs WHERE deleted_at IS NULL AND (?='' OR name LIKE ?)",Long.class,keyword,like);
        List<Map<String,Object>> items=jdbc.queryForList(
            "SELECT j.id,j.name,j.description,j.active,j.created_at AS createdAt,"+
                "COUNT(DISTINCT uj.user_id) AS userCount,COUNT(DISTINCT mj.matter_id) AS matterCount "+
                "FROM jobs j LEFT JOIN user_jobs uj ON uj.job_id=j.id AND uj.deleted_at IS NULL "+
                "LEFT JOIN discovery_matter_jobs mj ON mj.job_id=j.id AND mj.deleted_at IS NULL "+
                "WHERE j.deleted_at IS NULL AND (?='' OR j.name LIKE ?) "+
                "GROUP BY j.id,j.name,j.description,j.active,j.created_at "+
                "ORDER BY j.active DESC,j.name,j.id LIMIT ? OFFSET ?",
            keyword,like,size,page*size
        );
        return new PageResult(items,total,page,size);
    }
    public List<Map<String,Object>> jobOptions(){
        return jdbc.queryForList("SELECT j.id,j.name,j.description,j.active,COUNT(DISTINCT uj.user_id) AS userCount FROM jobs j LEFT JOIN user_jobs uj ON uj.job_id=j.id AND uj.deleted_at IS NULL WHERE j.deleted_at IS NULL GROUP BY j.id,j.name,j.description,j.active ORDER BY j.active DESC,j.name,j.id");
    }
    public List<Map<String,Object>> experienceOptions(){
        return jdbc.queryForList("SELECT e.id,e.name,e.category_id AS categoryId,c.name AS categoryName,c.main_category AS mainCategory,e.active FROM discovery_experiences e JOIN discovery_categories c ON c.id=e.category_id WHERE e.active=TRUE AND e.deleted_at IS NULL AND c.active=TRUE AND c.deleted_at IS NULL ORDER BY FIELD(c.main_category,'GENERAL','LIFE','WORK','ENTERTAINMENT'),c.sort_order,e.name,e.id");
    }
    public PageResult jobUsers(String jobName,Long jobId,int page,int size){
        String keyword=jobName==null?"":jobName.trim();
        String like="%"+keyword+"%";
        Long total=jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_jobs uj JOIN jobs j ON j.id=uj.job_id WHERE uj.deleted_at IS NULL AND j.deleted_at IS NULL AND (? IS NULL OR j.id=?) AND (?='' OR j.name LIKE ?)",
            Long.class,jobId,jobId,keyword,like
        );
        List<Map<String,Object>> items=jdbc.queryForList(
            "SELECT CONCAT(uj.user_id,'-',uj.job_id) AS relationId,j.id AS jobId,j.name AS jobName,u.id AS userId,u.uid,u.nickname,"+
                "u.avatar_url AS avatarUrl,u.account_status AS accountStatus,uj.verified,c.years "+
                "FROM user_jobs uj JOIN jobs j ON j.id=uj.job_id JOIN users u ON u.id=uj.user_id "+
                "LEFT JOIN certifications c ON c.id=uj.certification_id "+
                "WHERE uj.deleted_at IS NULL AND j.deleted_at IS NULL AND (? IS NULL OR j.id=?) AND (?='' OR j.name LIKE ?) ORDER BY j.name,u.id DESC LIMIT ? OFFSET ?",
            jobId,jobId,keyword,like,size,page*size
        );
        return new PageResult(items,total,page,size);
    }
    @Transactional public void createJob(AdminUser admin,String name,String description,String ip){String value=required(name,"岗位名称不能为空");ensureJobName(value,null);jdbc.update("INSERT INTO jobs(name,description,active) VALUES (?,?,TRUE)",value,cleanDescription(description));Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);audit(admin,"CREATE_JOB","JOB",id,value,ip);}
    @Transactional public void updateJob(AdminUser admin,Long id,String name,String description,Boolean active,String ip){String value=required(name,"岗位名称不能为空");ensureJobName(value,id);if(jdbc.update("UPDATE jobs SET name=?,description=?,active=? WHERE id=?",value,cleanDescription(description),active==null||active,id)!=1)throw BusinessException.notFound("岗位不存在");audit(admin,"UPDATE_JOB","JOB",id,value,ip);}
    @Transactional public void deleteJob(AdminUser admin,Long id,String ip){if(count("user_jobs","job_id="+id+" AND deleted_at IS NULL")>0||count("discovery_matter_jobs","job_id="+id+" AND deleted_at IS NULL")>0)throw BusinessException.badRequest("该岗位仍关联用户或事情，请先解除关联");if(jdbc.update("UPDATE jobs SET active=FALSE,deleted_at=NOW(6) WHERE id=? AND deleted_at IS NULL",id)!=1)throw BusinessException.notFound("岗位不存在");audit(admin,"DELETE_JOB","JOB",id,"SOFT_DELETE",ip);}
    @Transactional public void processWithdrawal(AdminUser admin,Long id,String status,String ip) {
        if(!List.of("COMPLETED","FAILED").contains(status)) throw BusinessException.badRequest("提现状态不正确");
        Map<String,Object> row=jdbc.queryForMap(
            "SELECT w.user_id,w.amount,w.status,u.account_type AS accountType FROM withdrawals w JOIN users u ON u.id=w.user_id WHERE w.id=? FOR UPDATE",
            id
        );
        if ("TEST".equals(row.get("accountType"))) {
            throw BusinessException.badRequest("测试提现由沙箱自动完成，不能进入真实出款处理");
        }
        if(!List.of("PROCESSING", "EXPORTED").contains(String.valueOf(row.get("status")))) {
            throw BusinessException.badRequest("该提现已经处理");
        }
        jdbc.update("UPDATE withdrawals SET status=?,completed_at=NOW(6) WHERE id=?",status,id);
        if("FAILED".equals(status)) {
            jdbc.update(
                "UPDATE wallet_accounts SET available_balance=available_balance+?,income_balance=income_balance+?,"+
                    "total_withdrawn=GREATEST(total_withdrawn-?,0) WHERE user_id=?",
                row.get("amount"),row.get("amount"),row.get("amount"),row.get("user_id")
            );
            jdbc.update("INSERT INTO wallet_transactions(user_id,transaction_type,direction,amount,available_after,frozen_after,reference_type,reference_id,description) SELECT ?, 'WITHDRAWAL_REFUND','IN',?,available_balance,frozen_balance,'WITHDRAWAL',?,'提现失败退款' FROM wallet_accounts WHERE user_id=?",row.get("user_id"),row.get("amount"),id,row.get("user_id"));
        }
        audit(admin,"PROCESS_WITHDRAWAL","WITHDRAWAL",id,status,ip);
        securityEvents.recordSafely(
            ((Number) row.get("user_id")).longValue(), admin.getId(),
            "WITHDRAWAL_" + status, "CRITICAL", ip, null,
            "withdrawalId=" + id + ", amount=" + row.get("amount")
        );
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
        return jdbc.queryForList("SELECT u.id AS userId,u.uid,u.nickname,u.avatar_url AS avatarUrl,MAX(m.created_at) AS lastMessageAt,SUBSTRING_INDEX(GROUP_CONCAT(CASE WHEN m.message_type='IMAGE' THEN '[图片]' ELSE m.content END ORDER BY m.id DESC SEPARATOR '\\n'),'\\n',1) AS lastMessage,SUM(CASE WHEN m.sender_type='USER' AND m.read_flag=FALSE THEN 1 ELSE 0 END) AS unread FROM customer_service_messages m JOIN users u ON u.id=m.user_id GROUP BY u.id,u.uid,u.nickname,u.avatar_url ORDER BY lastMessageAt DESC");
    }
    @Transactional public List<Map<String,Object>> customerServiceMessages(Long userId) {
        jdbc.update("UPDATE customer_service_messages SET read_flag=TRUE WHERE user_id=? AND sender_type='USER'",userId);
        return jdbc.queryForList("SELECT id,sender_type AS senderType,message_type AS messageType,content,attachment_url AS attachmentUrl,attachment_key AS attachmentKey,attachment_name AS attachmentName,attachment_size AS attachmentSize,created_at AS createdAt FROM customer_service_messages WHERE user_id=? ORDER BY id",userId)
            .stream().map(this::withPrivateAttachmentUrl).toList();
    }
    @Transactional public void readCustomerServiceMessages(Long userId) {
        jdbc.update("UPDATE customer_service_messages SET read_flag=TRUE WHERE user_id=? AND sender_type='USER'",userId);
    }
    @Transactional public Map<String,Object> replyCustomerService(AdminUser admin,Long userId,String content,String ip) {
        String value=required(content,"回复内容不能为空");
        if(count("users","id="+userId)==0) throw BusinessException.notFound("用户不存在");
        jdbc.update("INSERT INTO customer_service_messages(user_id,sender_type,message_type,content,read_flag) VALUES (?,'SERVICE','TEXT',?,FALSE)",userId,value);
        Long messageId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        Map<String,Object> saved=withPrivateAttachmentUrl(jdbc.queryForMap("SELECT id,sender_type AS senderType,message_type AS messageType,content,attachment_url AS attachmentUrl,attachment_key AS attachmentKey,attachment_name AS attachmentName,attachment_size AS attachmentSize,created_at AS createdAt FROM customer_service_messages WHERE id=?",messageId));
        realtime.afterCommit(userId,"CUSTOMER_SERVICE_MESSAGE",saved);
        audit(admin,"REPLY_CUSTOMER_SERVICE","USER",userId,value,ip);
        return saved;
    }

    private Map<String,Object> withPrivateAttachmentUrl(Map<String,Object> source) {
        Map<String,Object> item = new LinkedHashMap<>(source);
        String storageKey = Objects.toString(item.remove("attachmentKey"), "");
        if (!storageKey.isBlank()) {
            item.put("attachmentUrl", fileStorage.accessUrl(storageKey, StorageVisibility.PRIVATE));
        }
        return item;
    }
    public Map<String,Object> discovery() {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("categories",jdbc.queryForList("SELECT id,main_category AS mainCategory,name,content_scope AS contentScope,sort_order AS sortOrder,active FROM discovery_categories WHERE deleted_at IS NULL ORDER BY FIELD(main_category,'GENERAL','LIFE','WORK','ENTERTAINMENT'),sort_order,id"));
        result.put("matters",jdbc.queryForList("SELECT m.id,m.category_id AS categoryId,m.title,m.sort_order AS sortOrder,m.active,c.main_category AS mainCategory,c.name AS categoryName FROM discovery_matters m JOIN discovery_categories c ON c.id=m.category_id WHERE m.deleted_at IS NULL AND c.deleted_at IS NULL ORDER BY FIELD(c.main_category,'GENERAL','LIFE','WORK','ENTERTAINMENT'),c.sort_order,m.sort_order,m.id"));
        result.put("matterJobs",jdbc.queryForList("SELECT mj.matter_id AS matterId,mj.job_id AS jobId,mj.sort_order AS sortOrder,j.name AS jobName FROM discovery_matter_jobs mj JOIN jobs j ON j.id=mj.job_id WHERE mj.active=TRUE AND mj.deleted_at IS NULL AND j.deleted_at IS NULL ORDER BY mj.matter_id,mj.sort_order,j.name"));
        result.put("jobs",jobOptions());
        result.put("experienceCatalogs",jdbc.queryForList("SELECT e.id,e.category_id AS categoryId,e.name,e.active,c.main_category AS mainCategory,c.name AS categoryName,COUNT(DISTINCT t.user_id) AS userCount FROM discovery_experiences e JOIN discovery_categories c ON c.id=e.category_id LEFT JOIN certifications t ON t.discovery_experience_id=e.id AND t.category='EXPERIENCE' AND t.status='APPROVED' AND t.deleted_at IS NULL WHERE e.deleted_at IS NULL AND c.deleted_at IS NULL GROUP BY e.id,e.category_id,e.name,e.active,c.main_category,c.name ORDER BY FIELD(c.main_category,'GENERAL','LIFE','WORK','ENTERTAINMENT'),c.sort_order,e.name,e.id"));
        return result;
    }
    public PageResult experienceUsers(Long experienceId,int page,int size){
        if(count("discovery_experiences","id="+experienceId+" AND deleted_at IS NULL")==0) throw BusinessException.notFound("经历不存在");
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM certifications t WHERE t.category='EXPERIENCE' AND t.status='APPROVED' AND t.deleted_at IS NULL AND t.discovery_experience_id=?",Long.class,experienceId);
        List<Map<String,Object>> items=jdbc.queryForList(
            "SELECT t.id,t.title,t.description,t.enabled,t.reviewed_at AS reviewedAt,u.id AS userId,u.uid,u.nickname,u.avatar_url AS avatarUrl,u.account_status AS accountStatus " +
                "FROM certifications t JOIN users u ON u.id=t.user_id WHERE t.category='EXPERIENCE' AND t.status='APPROVED' AND t.deleted_at IS NULL AND t.discovery_experience_id=? ORDER BY t.id DESC LIMIT ? OFFSET ?",
            experienceId,size,page*size
        );
        return new PageResult(items,total,page,size);
    }
    @Transactional public void createCategory(AdminUser admin,String mainCategory,String name,Integer sortOrder,String ip) {
        String main=mainCategory(mainCategory), value=required(name,"子分类名称不能为空");
        if("GENERAL".equals(main)) throw BusinessException.badRequest("通用大类只包含固定分类");
        ensureCategoryNameAvailable(main,value,null);
        int tail=jdbc.queryForObject("SELECT COUNT(*)+1 FROM discovery_categories WHERE main_category=? AND deleted_at IS NULL",Integer.class,main);
        jdbc.update("INSERT INTO discovery_categories(main_category,name,sort_order,active) VALUES (?,?,?,TRUE)",main,value,tail);
        Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        reorderCategories(main,id,sortOrder==null?tail:sortOrder);
        audit(admin,"CREATE_DISCOVERY_CATEGORY","DISCOVERY_CATEGORY",id,main+"/"+value,ip);
    }
    @Transactional public void updateCategory(AdminUser admin,Long id,String mainCategory,String name,Integer sortOrder,Boolean active,String ip) {
        String main=mainCategory(mainCategory), value=required(name,"子分类名称不能为空");
        String oldMain=jdbc.query("SELECT main_category FROM discovery_categories WHERE id=? AND deleted_at IS NULL",rs->rs.next()?rs.getString(1):null,id);
        if(oldMain==null) throw BusinessException.notFound("子分类不存在");
        if("GENERAL".equals(oldMain)||"GENERAL".equals(main)) throw BusinessException.badRequest("通用大类的固定分类不能修改");
        ensureCategoryNameAvailable(main,value,id);
        jdbc.update("UPDATE discovery_categories SET main_category=?,name=?,active=? WHERE id=?",main,value,active==null||active,id);
        if(!oldMain.equals(main)) normalizeCategories(oldMain);
        reorderCategories(main,id,sortOrder==null?Integer.MAX_VALUE:sortOrder);
        audit(admin,"UPDATE_DISCOVERY_CATEGORY","DISCOVERY_CATEGORY",id,main+"/"+value,ip);
    }
    @Transactional public void deleteCategory(AdminUser admin,Long id,String ip) {
        if(count("discovery_matters","category_id="+id+" AND deleted_at IS NULL")>0) throw BusinessException.badRequest("该分类下仍有事情，请先删除或转移事情");
        if(count("certifications","discovery_category_id="+id+" AND deleted_at IS NULL")>0) throw BusinessException.badRequest("该分类仍关联认证经历，请先取消或调整经历分类");
        if(count("discovery_experiences","category_id="+id+" AND deleted_at IS NULL")>0) throw BusinessException.badRequest("该分类下仍有标准经历，请先删除或转移标准经历");
        String main=jdbc.query("SELECT main_category FROM discovery_categories WHERE id=? AND deleted_at IS NULL",rs->rs.next()?rs.getString(1):null,id);
        if("GENERAL".equals(main)) throw BusinessException.badRequest("通用大类的固定分类不能删除");
        if(main==null||jdbc.update("UPDATE discovery_categories SET active=FALSE,deleted_at=NOW(6) WHERE id=? AND deleted_at IS NULL",id)!=1) throw BusinessException.notFound("子分类不存在");
        normalizeCategories(main);
        audit(admin,"DELETE_DISCOVERY_CATEGORY","DISCOVERY_CATEGORY",id,"SOFT_DELETE",ip);
    }
    @Transactional public void createMatter(AdminUser admin,Long categoryId,String title,Integer sortOrder,List<AdminManagementController.MatterJobRequest> jobs,String ip) {
        requireCategory(categoryId,"MATTERS"); String value=required(title,"事情名称不能为空");
        ensureMatterTitleAvailable(categoryId,value,null);
        int tail=jdbc.queryForObject("SELECT COUNT(*)+1 FROM discovery_matters WHERE category_id=? AND deleted_at IS NULL",Integer.class,categoryId);
        jdbc.update("INSERT INTO discovery_matters(category_id,title,sort_order,active) VALUES (?,?,?,TRUE)",categoryId,value,tail);
        Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        replaceMatterJobs(id,jobs);
        reorderMatters(categoryId,id,sortOrder==null?tail:sortOrder);
        audit(admin,"CREATE_DISCOVERY_MATTER","DISCOVERY_MATTER",id,value,ip);
    }
    @Transactional public void updateMatter(AdminUser admin,Long id,Long categoryId,String title,Integer sortOrder,Boolean active,List<AdminManagementController.MatterJobRequest> jobs,String ip) {
        requireCategory(categoryId,"MATTERS"); String value=required(title,"事情名称不能为空");
        ensureMatterTitleAvailable(categoryId,value,id);
        Long oldCategory=jdbc.query("SELECT category_id FROM discovery_matters WHERE id=? AND deleted_at IS NULL",rs->rs.next()?rs.getLong(1):null,id);
        if(oldCategory==null) throw BusinessException.notFound("事情不存在");
        jdbc.update("UPDATE discovery_matters SET category_id=?,title=?,active=? WHERE id=?",categoryId,value,active==null||active,id);
        replaceMatterJobs(id,jobs);
        if(!oldCategory.equals(categoryId)) normalizeMatters(oldCategory);
        reorderMatters(categoryId,id,sortOrder==null?Integer.MAX_VALUE:sortOrder);
        audit(admin,"UPDATE_DISCOVERY_MATTER","DISCOVERY_MATTER",id,value,ip);
    }
    @Transactional public void deleteMatter(AdminUser admin,Long id,String ip) {
        Long categoryId=jdbc.query("SELECT category_id FROM discovery_matters WHERE id=? AND deleted_at IS NULL",rs->rs.next()?rs.getLong(1):null,id);
        if(categoryId==null||jdbc.update("UPDATE discovery_matters SET active=FALSE,deleted_at=NOW(6) WHERE id=? AND deleted_at IS NULL",id)!=1) throw BusinessException.notFound("事情不存在");
        normalizeMatters(categoryId);
        audit(admin,"DELETE_DISCOVERY_MATTER","DISCOVERY_MATTER",id,"SOFT_DELETE",ip);
    }
    @Transactional public void createExperience(AdminUser admin,Long categoryId,String name,String ip){
        requireCategory(categoryId,"EXPERIENCES"); String value=required(name,"标准经历名称不能为空"); ensureExperienceName(categoryId,value,null);
        jdbc.update("INSERT INTO discovery_experiences(category_id,name,active) VALUES (?,?,TRUE)",categoryId,value);
        Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class); audit(admin,"CREATE_DISCOVERY_EXPERIENCE","DISCOVERY_EXPERIENCE",id,value,ip);
    }
    @Transactional public void updateExperience(AdminUser admin,Long id,Long categoryId,String name,Boolean active,String ip){
        requireCategory(categoryId,"EXPERIENCES"); String value=required(name,"标准经历名称不能为空"); ensureExperienceName(categoryId,value,id);
        if(jdbc.update("UPDATE discovery_experiences SET category_id=?,name=?,active=? WHERE id=?",categoryId,value,active==null||active,id)!=1) throw BusinessException.notFound("标准经历不存在");
        jdbc.update("UPDATE certifications SET discovery_category_id=? WHERE discovery_experience_id=?",categoryId,id);
        audit(admin,"UPDATE_DISCOVERY_EXPERIENCE","DISCOVERY_EXPERIENCE",id,value,ip);
    }
    @Transactional public void deleteExperience(AdminUser admin,Long id,String ip){
        if(count("certifications","discovery_experience_id="+id+" AND deleted_at IS NULL")>0) throw BusinessException.badRequest("该标准经历仍关联用户，不能删除");
        if(jdbc.update("UPDATE discovery_experiences SET active=FALSE,deleted_at=NOW(6) WHERE id=? AND deleted_at IS NULL",id)!=1) throw BusinessException.notFound("标准经历不存在");
        audit(admin,"DELETE_DISCOVERY_EXPERIENCE","DISCOVERY_EXPERIENCE",id,"SOFT_DELETE",ip);
    }
    @Transactional public void classifyExperience(AdminUser admin,Long id,Long experienceId,String ip) {
        if(experienceId==null){
            if(jdbc.update("UPDATE certifications SET discovery_category_id=NULL,discovery_experience_id=NULL WHERE id=? AND category='EXPERIENCE' AND status='APPROVED'",id)!=1) throw BusinessException.badRequest("仅可调整已审核通过的亲身经历");
        }else{
            requireExperience(experienceId);
            if(jdbc.update("UPDATE certifications c JOIN discovery_experiences e ON e.id=? SET c.discovery_experience_id=e.id,c.discovery_category_id=e.category_id WHERE c.id=? AND c.category='EXPERIENCE' AND c.status='APPROVED'",experienceId,id)!=1) throw BusinessException.badRequest("仅可调整已审核通过的亲身经历");
        }
        audit(admin,"CLASSIFY_EXPERIENCE","CERTIFICATION",id,String.valueOf(experienceId),ip);
    }
    private void requireExperience(Long id){if(id==null||count("discovery_experiences","id="+id+" AND active=TRUE AND deleted_at IS NULL")==0) throw BusinessException.badRequest("请选择有效的标准经历");}
    private void ensureExperienceName(Long categoryId,String name,Long excludedId){Long total=excludedId==null?jdbc.queryForObject("SELECT COUNT(*) FROM discovery_experiences WHERE category_id=? AND name=?",Long.class,categoryId,name):jdbc.queryForObject("SELECT COUNT(*) FROM discovery_experiences WHERE category_id=? AND name=? AND id<>?",Long.class,categoryId,name,excludedId);if(total!=null&&total>0)throw BusinessException.badRequest("该分类下已存在同名标准经历");}
    private void requireCategory(Long id,String contentScope){
        Long total=id==null?0L:jdbc.queryForObject(
            "SELECT COUNT(*) FROM discovery_categories WHERE id=? AND active=TRUE AND deleted_at IS NULL AND content_scope IN ('BOTH',?)",
            Long.class,id,contentScope
        );
        if(total==null||total==0) throw BusinessException.badRequest("请选择有效的子分类");
    }
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
        jdbc.update("UPDATE discovery_matter_jobs SET active=FALSE,deleted_at=NOW(6) WHERE matter_id=? AND deleted_at IS NULL",matterId);
        if(jobs==null||jobs.isEmpty()) throw BusinessException.badRequest("每件事情至少设置一个岗位");
        int order=1;
        Set<Long> seen=new HashSet<>();
        for(AdminManagementController.MatterJobRequest job:jobs){
            if(job==null||job.jobId()==null) continue;
            if(!seen.add(job.jobId())) throw BusinessException.badRequest("同一岗位不能重复配置");
            if(count("jobs","id="+job.jobId()+" AND active=TRUE")==0) throw BusinessException.badRequest("所选岗位不存在或已停用");
            jdbc.update("INSERT INTO discovery_matter_jobs(matter_id,job_id,sort_order,active,deleted_at) VALUES (?,?,?,TRUE,NULL) ON DUPLICATE KEY UPDATE sort_order=VALUES(sort_order),active=TRUE,deleted_at=NULL",matterId,job.jobId(),order++);
        }
        if(order==1) throw BusinessException.badRequest("每件事情至少设置一个岗位");
    }
    private void ensureJobName(String name,Long excludedId){Long total=excludedId==null?jdbc.queryForObject("SELECT COUNT(*) FROM jobs WHERE name=?",Long.class,name):jdbc.queryForObject("SELECT COUNT(*) FROM jobs WHERE name=? AND id<>?",Long.class,name,excludedId);if(total!=null&&total>0)throw BusinessException.badRequest("岗位名称已存在");}
    private String cleanDescription(String value){if(value==null||value.isBlank())return null;String result=sensitiveWords.mask(value.trim());if(result.length()>240)throw BusinessException.badRequest("介绍不能超过240个字");return result;}
    private String mainCategory(String value){String result=value==null?"":value.trim().toUpperCase(Locale.ROOT);if(!List.of("GENERAL","LIFE","WORK","ENTERTAINMENT").contains(result)) throw BusinessException.badRequest("主分类不正确");return result;}
    private String required(String value,String message){if(value==null||value.isBlank()) throw BusinessException.badRequest(message);return sensitiveWords.mask(value.trim());}
    private void reorderCategories(String main,Long movingId,int requestedPosition){
        List<Long> ids=jdbc.queryForList("SELECT id FROM discovery_categories WHERE main_category=? AND deleted_at IS NULL AND id<>? ORDER BY sort_order,id",Long.class,main,movingId);
        ids.add(Math.max(0,Math.min(requestedPosition-1,ids.size())),movingId);
        for(int index=0;index<ids.size();index++) jdbc.update("UPDATE discovery_categories SET sort_order=? WHERE id=?",index+1,ids.get(index));
    }
    private void normalizeCategories(String main){List<Long> ids=jdbc.queryForList("SELECT id FROM discovery_categories WHERE main_category=? AND deleted_at IS NULL ORDER BY sort_order,id",Long.class,main);for(int i=0;i<ids.size();i++) jdbc.update("UPDATE discovery_categories SET sort_order=? WHERE id=?",i+1,ids.get(i));}
    private void reorderMatters(Long categoryId,Long movingId,int requestedPosition){
        List<Long> ids=jdbc.queryForList("SELECT id FROM discovery_matters WHERE category_id=? AND deleted_at IS NULL AND id<>? ORDER BY sort_order,id",Long.class,categoryId,movingId);
        ids.add(Math.max(0,Math.min(requestedPosition-1,ids.size())),movingId);
        for(int index=0;index<ids.size();index++) jdbc.update("UPDATE discovery_matters SET sort_order=? WHERE id=?",index+1,ids.get(index));
    }
    private void normalizeMatters(Long categoryId){List<Long> ids=jdbc.queryForList("SELECT id FROM discovery_matters WHERE category_id=? AND deleted_at IS NULL ORDER BY sort_order,id",Long.class,categoryId);for(int i=0;i<ids.size();i++) jdbc.update("UPDATE discovery_matters SET sort_order=? WHERE id=?",i+1,ids.get(i));}
    private long count(String table,String where){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+where,Long.class);}
    private void audit(AdminUser admin,String action,String type,Object id,String detail,String ip){ AdminAuditLog l=new AdminAuditLog();l.setAdminUser(admin);l.setAction(action);l.setTargetType(type);l.setTargetId(String.valueOf(id));l.setDetail(detail);l.setIpAddress(ip);audits.save(l); }
    private TableSpec spec(String type){return switch(type){
        case "certifications" -> new TableSpec(
            "certifications",
            "certifications t JOIN users u ON u.id=t.user_id",
            "SELECT t.id,t.category,t.certification_type AS type,t.title,t.description,t.years,t.status,t.enabled,t.rejection_reason AS rejectionReason,t.submitted_at AS submittedAt,u.uid,u.nickname,(u.account_type='TEST') AS testData FROM certifications t JOIN users u ON u.id=t.user_id",
            List.of("u")
        );
        case "inquiries" -> new TableSpec(
            "inquiries",
            "inquiries t JOIN users q ON q.id=t.questioner_id JOIN users a ON a.id=t.answerer_id",
            "SELECT t.id,t.topic,t.question,t.amount,t.client_platform AS clientPlatform,t.service_fee_rate AS serviceFeeRate,t.service_fee_amount AS serviceFeeAmount,t.answerer_income_amount AS answererIncomeAmount,t.status,t.funds_status AS fundsStatus,t.created_at AS createdAt,q.uid AS questionerUid,a.uid AS answererUid,(q.account_type='TEST') AS testData FROM inquiries t JOIN users q ON q.id=t.questioner_id JOIN users a ON a.id=t.answerer_id",
            List.of("q", "a")
        );
        case "withdrawals" -> new TableSpec(
            "withdrawals",
            "withdrawals t JOIN users u ON u.id=t.user_id",
            "SELECT t.id,t.amount,t.payee_name_snapshot AS payeeName,t.alipay_account_masked_snapshot AS alipayAccount,t.status,t.batch_no AS batchNo,t.exported_at AS exportedAt,t.created_at AS createdAt,u.uid,u.nickname,(u.account_type='TEST') AS testData FROM withdrawals t JOIN users u ON u.id=t.user_id",
            List.of("u")
        );
        case "feedback" -> new TableSpec(
            "feedback_records",
            "feedback_records t JOIN users u ON u.id=t.user_id LEFT JOIN users tu ON tu.id=t.target_user_id",
            "SELECT t.id,t.feedback_type AS type,t.category,t.content,t.status,t.created_at AS createdAt,u.uid,u.nickname,tu.uid AS targetUid,(u.account_type='TEST') AS testData FROM feedback_records t JOIN users u ON u.id=t.user_id LEFT JOIN users tu ON tu.id=t.target_user_id",
            List.of("u", "tu")
        );
        case "cooperations" -> new TableSpec(
            "business_cooperations",
            "business_cooperations t JOIN users u ON u.id=t.user_id",
            "SELECT t.id,t.contact,t.content,t.status,t.created_at AS createdAt,u.uid,u.nickname,(u.account_type='TEST') AS testData FROM business_cooperations t JOIN users u ON u.id=t.user_id",
            List.of("u")
        );
        default -> throw BusinessException.badRequest("管理模块不存在");};}
    private record TableSpec(String table,String from,String select,List<String> userAliases){}
    public record PageResult(List<Map<String,Object>> items,long total,int page,int size){}
}

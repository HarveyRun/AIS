package com.shixianwen.admin;

import com.shixianwen.certification.CertificationService;
import com.shixianwen.certification.CertificationPublicMediaService;
import com.shixianwen.certification.ExperienceReviewScore;
import com.shixianwen.common.BusinessException;
import com.shixianwen.config.AppGlobalSettingService;
import com.shixianwen.content.SensitiveWordService;
import com.shixianwen.storage.FileStorage;
import com.shixianwen.storage.StorageVisibility;
import com.shixianwen.security.SecurityEventService;
import com.shixianwen.wallet.PlatformServiceFeePolicy;
import com.shixianwen.wallet.PermanentBanPayoutService;
import com.shixianwen.notification.NotificationService;
import com.shixianwen.user.UserRepository;
import com.shixianwen.finance.FinancialLedgerService;
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
    private final CertificationPublicMediaService certificationPublicMedia;
    private final AdminAuditLogRepository audits;
    private final RealtimePublisher realtime;
    private final SensitiveWordService sensitiveWords;
    private final FileStorage fileStorage;
    private final SecurityEventService securityEvents;
    private final PlatformServiceFeePolicy platformServiceFeePolicy;
    private final FinancialLedgerService financialLedger;
    private final PermanentBanPayoutService permanentBanPayouts;
    private final NotificationService notifications;
    private final UserRepository users;
    private final AppGlobalSettingService appGlobalSettings;

    public Map<String,Object> dashboard() {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("users", count("users","account_status='ACTIVE' AND account_type='NORMAL'"));
        result.put("answerers", jdbc.queryForObject(
            "SELECT COUNT(*) FROM users u WHERE u.account_status='ACTIVE' AND u.account_type='NORMAL' " +
                "AND EXISTS (SELECT 1 FROM certifications ce WHERE ce.user_id=u.id AND ce.category='EXPERIENCE' AND ce.status='APPROVED' AND ce.enabled=TRUE AND ce.deleted_at IS NULL)",
            Long.class
        ));
        result.put("approvedExperiences", jdbc.queryForObject(
            "SELECT COUNT(*) FROM certifications c JOIN users u ON u.id=c.user_id WHERE c.category='EXPERIENCE' AND c.status='APPROVED' AND c.enabled=TRUE AND c.deleted_at IS NULL AND u.account_type='NORMAL'",
            Long.class
        ));
        result.put("pendingExperiences", jdbc.queryForObject(
            "SELECT COUNT(*) FROM certifications c JOIN users u ON u.id=c.user_id WHERE c.category='EXPERIENCE' AND c.status='PENDING' AND c.deleted_at IS NULL AND u.account_type='NORMAL'",
            Long.class
        ));
        result.put("pendingCertifications", jdbc.queryForObject(
            "SELECT COUNT(*) FROM certifications c JOIN users u ON u.id=c.user_id WHERE c.status='PENDING' AND c.deleted_at IS NULL AND u.account_type='NORMAL'",
            Long.class
        ));
        result.put("activeInquiries", jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiries i JOIN users u ON u.id=i.questioner_id WHERE i.status IN ('PENDING','ACTIVE','TEXT_LIMIT_REACHED') AND u.account_type='NORMAL'",
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
        result.put("pendingAudioRequests", count(
            "inquiry_voice_calls",
            "status='CONNECTING'"
        ));
        result.put("inquiriesExpiringWithin24Hours", jdbc.queryForObject(
            "SELECT COUNT(*) FROM inquiries WHERE status IN ('ACTIVE','TEXT_LIMIT_REACHED') " +
                "AND conversation_expires_at>NOW(6) AND conversation_expires_at<=DATE_ADD(NOW(6),INTERVAL 24 HOUR)",
            Long.class
        ));
        result.put("voiceCallsToday", jdbc.queryForObject(
            "SELECT COUNT(DISTINCT voice_call_id) FROM voice_call_events WHERE event_type='PEER_CONNECTED' AND created_at>=CURRENT_DATE()",
            Long.class
        ));
        result.put("voiceConnectionFailuresToday", jdbc.queryForObject(
            "SELECT COUNT(*) FROM voice_call_events WHERE event_type IN ('CONNECT_TIMEOUT','RECONNECT_TIMEOUT') AND created_at>=CURRENT_DATE()",
            Long.class
        ));
        result.put("mediaProcessingFailures", count(
            "certifications",
            "media_processing_status='FAILED' AND deleted_at IS NULL"
        ));
        result.put("feedbackOver24Hours", jdbc.queryForObject(
            "SELECT COUNT(*) FROM feedback_records WHERE status IN ('SUBMITTED','PROCESSING') AND created_at<DATE_SUB(NOW(6),INTERVAL 24 HOUR)",
            Long.class
        ));
        result.put("riskyWithdrawals", count(
            "withdrawals",
            "status IN ('PROCESSING','EXPORTED') AND risk_level<>'LOW'"
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

    public Map<String, Object> inquiryCapacitySetting() {
        return jdbc.queryForMap(
            "SELECT questioner_active_limit AS questionerLimit," +
                "answerer_active_limit AS answererLimit,updated_at AS updatedAt " +
                "FROM inquiry_capacity_settings WHERE id=1"
        );
    }

    public AppGlobalSettingService.Settings appGlobalSetting() {
        return appGlobalSettings.current();
    }

    @Transactional
    public AppGlobalSettingService.Settings updateAppGlobalSetting(
        AdminUser admin,
        AppGlobalSettingService.Settings value,
        String ip
    ) {
        AppGlobalSettingService.Settings result = appGlobalSettings.update(value, admin.getId());
        audit(
            admin,
            "UPDATE_APP_GLOBAL_SETTING",
            "APP_GLOBAL_SETTING",
            "1",
            "修改App全局业务规则",
            ip
        );
        realtime.afterCommitToAllUsers(
            "APP_GLOBAL_SETTINGS_UPDATED",
            Map.of("updatedAt", result.updatedAt().toString())
        );
        return result;
    }

    @Transactional
    public Map<String, Object> updateInquiryCapacity(
        AdminUser admin,
        Integer questionerLimit,
        Integer answererLimit,
        String ip
    ) {
        int normalizedQuestioner = capacityLimit(questionerLimit, "询问人");
        int normalizedAnswerer = capacityLimit(answererLimit, "被询问人");
        int changed = jdbc.update(
            "UPDATE inquiry_capacity_settings SET questioner_active_limit=?," +
                "answerer_active_limit=?,updated_by_admin_id=? WHERE id=1",
            normalizedQuestioner,
            normalizedAnswerer,
            admin.getId()
        );
        if (changed != 1) throw BusinessException.badRequest("询问容量配置不存在");
        audit(
            admin,
            "UPDATE_INQUIRY_CAPACITY",
            "INQUIRY_CAPACITY_SETTING",
            "1",
            "询问人=" + normalizedQuestioner + "，被询问人=" + normalizedAnswerer,
            ip
        );
        return inquiryCapacitySetting();
    }

    private int capacityLimit(Integer value, String label) {
        if (value == null || value < 1 || value > 20) {
            throw BusinessException.badRequest(label + "同时交流上限必须在1至20之间");
        }
        return value;
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
            where.append(" AND (?='' OR t.category=?) AND t.certification_type IN ('EXPERIENCE','IDENTITY') AND t.deleted_at IS NULL");
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
            "SELECT cm.id," +
                "CASE WHEN cm.material_kind IN ('ARCHIVE','PROOF_ARCHIVE','REVIEW_ORIGINAL_ARCHIVE') " +
                "THEN 'PROOF_ARCHIVE' ELSE cm.material_kind END AS kind," +
                "cm.original_name AS name,cm.storage_key AS storageKey,cm.public_url AS publicUrl," +
                "cm.content_type AS contentType,cm.file_size AS size " +
                "FROM certification_materials cm WHERE cm.certification_id=? AND cm.deleted_at IS NULL " +
                "AND (cm.material_kind NOT IN ('ARCHIVE','PROOF_ARCHIVE','REVIEW_ORIGINAL_ARCHIVE') " +
                "OR cm.id=(SELECT COALESCE(" +
                "MAX(CASE WHEN candidate.material_kind IN ('ARCHIVE','PROOF_ARCHIVE') THEN candidate.id END)," +
                "MAX(CASE WHEN candidate.material_kind='REVIEW_ORIGINAL_ARCHIVE' THEN candidate.id END)) " +
                "FROM certification_materials candidate WHERE candidate.certification_id=cm.certification_id " +
                "AND candidate.deleted_at IS NULL)) ORDER BY cm.id",
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
    @Transactional
    public void reviewCertification(
        AdminUser admin,
        Long id,
        boolean approved,
        String reason,
        Integer materialSupportScore,
        Integer commonRelevanceScore,
        Integer learnabilityScore,
        Integer clarityScore,
        Integer logicConsistencyScore,
        Integer informationSpecificityScore,
        String ip
    ) {
        Map<String,Object> certification=jdbc.queryForMap("SELECT user_id AS userId,category,certification_type AS type,title,status FROM certifications WHERE id=? AND deleted_at IS NULL FOR UPDATE",id);
        if(!"PENDING".equals(certification.get("status"))) throw BusinessException.badRequest("该认证已经处理");
        if (!List.of("EXPERIENCE", "IDENTITY").contains(String.valueOf(certification.get("type")))) {
            throw BusinessException.badRequest("该认证类型已停止使用");
        }
        boolean experience = "EXPERIENCE".equals(certification.get("category"));
        if (!experience && !approved && (reason == null || reason.isBlank())) {
            throw BusinessException.badRequest("驳回时请填写原因");
        }
        ExperienceReviewScore score = experience
            ? ExperienceReviewScore.of(
                materialSupportScore,
                commonRelevanceScore,
                learnabilityScore,
                clarityScore,
                logicConsistencyScore,
                informationSpecificityScore
            )
            : null;
        boolean resolvedApproved = score == null ? approved : score.approved();
        String resolvedReason = score != null && !resolvedApproved
            ? "信息具体程度评分为" + score.informationSpecificity() + "分，未达到审核通过标准"
            : reason;
        certifications.review(id,resolvedApproved,resolvedReason,score);
        if (score != null) {
            jdbc.update(
                "INSERT INTO experience_review_score_history(" +
                    "certification_id,reviewer_admin_id,material_support_score," +
                    "common_relevance_score,learnability_score,clarity_score," +
                    "logic_consistency_score,information_specificity_score,reference_index" +
                    ") VALUES(?,?,?,?,?,?,?,?,?)",
                id,
                admin.getId(),
                score.materialSupport(),
                score.commonRelevance(),
                score.learnability(),
                score.clarity(),
                score.logicConsistency(),
                score.informationSpecificity(),
                score.referenceIndex()
            );
        }
        audit(
            admin,"REVIEW_CERTIFICATION","CERTIFICATION",id,
            score == null
                ? String.valueOf(resolvedApproved)
                : "approved=" + resolvedApproved +
                    ",informationSpecificity=" + score.informationSpecificity() +
                    ",referenceIndex=" + score.referenceIndex(),
            ip
        );
        realtime.afterCommit(
            ((Number) certification.get("userId")).longValue(),
            "CERTIFICATION_UPDATED",
            Map.of(
                "id", id,
                "type", certification.get("type"),
                "status", resolvedApproved ? "APPROVED" : "REJECTED"
            )
        );
    }
    @Transactional public void retryCertificationMedia(AdminUser admin,Long id,String ip) {
        certificationPublicMedia.retry(id);
        audit(admin,"RETRY_CERTIFICATION_MEDIA","CERTIFICATION",id,"RETRY",ip);
    }
    @Transactional public void setCertificationEnabled(AdminUser admin,Long id,boolean enabled,String ip){
        Map<String,Object> item=certificationForUpdate(id);
        if(!"APPROVED".equals(item.get("status"))) throw BusinessException.badRequest("仅可调整已通过的认证");
        jdbc.update("UPDATE certifications SET enabled=? WHERE id=?",enabled,id);
        // 恢复经历只恢复询问资格，不替用户打开“接受新询问”。
        syncAnswererStatus(((Number)item.get("userId")).longValue(),false);
        audit(admin,"CHANGE_CERTIFICATION_ENABLED","CERTIFICATION",id,String.valueOf(enabled),ip);
    }
    @Transactional public void deleteCertification(AdminUser admin,Long id,String ip){
        Map<String,Object> item=certificationForUpdate(id);
        jdbc.update("UPDATE certifications SET enabled=FALSE,deleted_at=NOW(6) WHERE id=?",id);
        syncAnswererStatus(((Number)item.get("userId")).longValue(),false);
        audit(admin,"DELETE_CERTIFICATION","CERTIFICATION",id,"SOFT_DELETE",ip);
    }
    private Map<String,Object> certificationForUpdate(Long id){
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT user_id AS userId,certification_type AS type,status,enabled FROM certifications WHERE id=? AND deleted_at IS NULL FOR UPDATE",id);
        if(rows.isEmpty()) throw BusinessException.notFound("认证不存在"); return rows.get(0);
    }
    private void syncAnswererStatus(Long userId,boolean allowEnable){
        Long experience=jdbc.queryForObject("SELECT COUNT(*) FROM certifications WHERE user_id=? AND category='EXPERIENCE' AND status='APPROVED' AND enabled=TRUE AND deleted_at IS NULL",Long.class,userId);
        boolean approved=experience!=null&&experience>0;
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
            case "MONTHS_1" -> LocalDateTime.now().plusMonths(1);
            case "MONTHS_3" -> LocalDateTime.now().plusMonths(3);
            case "MONTHS_6" -> LocalDateTime.now().plusMonths(6);
            case "YEARS_1" -> LocalDateTime.now().plusYears(1);
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

        if (banUntil == null) {
            permanentBanPayouts.captureForPermanentBan(id);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", "违反平台规则");
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
            BigDecimal amount=(BigDecimal)row.get("amount");
            Long userId=((Number)row.get("user_id")).longValue();
            financialLedger.record(
                "WITHDRAWAL",id,"FAILED","提现失败退回",
                List.of(
                    FinancialLedgerService.entry("WITHDRAWAL_PAYABLE",userId,amount),
                    FinancialLedgerService.entry("USER_INCOME_LIABILITY",userId,FinancialLedgerService.negative(amount))
                )
            );
        } else {
            BigDecimal amount=(BigDecimal)row.get("amount");
            Long userId=((Number)row.get("user_id")).longValue();
            financialLedger.record(
                "WITHDRAWAL",id,"SUCCESS","提现完成",
                List.of(
                    FinancialLedgerService.entry("WITHDRAWAL_PAYABLE",userId,amount),
                    FinancialLedgerService.entry("ALIPAY_CLEARING",null,FinancialLedgerService.negative(amount))
                )
            );
        }
        audit(admin,"PROCESS_WITHDRAWAL","WITHDRAWAL",id,status,ip);
        securityEvents.recordSafely(
            ((Number) row.get("user_id")).longValue(), admin.getId(),
            "WITHDRAWAL_" + status, "CRITICAL", ip, null,
            "withdrawalId=" + id + ", amount=" + row.get("amount")
        );
    }
    @Transactional public void updateRecordStatus(AdminUser admin,String type,Long id,String status,String resolution,String ip) {
        TableSpec spec=spec(type); if(!List.of("PROCESSING","RESOLVED","CLOSED").contains(status)) throw BusinessException.badRequest("处理状态不正确");
        if ("feedback".equals(type) && List.of("RESOLVED", "CLOSED").contains(status)) {
            String result = required(resolution, "请填写处理结果");
            Map<String,Object> row;
            try {
                row = jdbc.queryForMap("SELECT user_id,feedback_type FROM feedback_records WHERE id=?", id);
            } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
                throw BusinessException.notFound("记录不存在");
            }
            if(jdbc.update(
                "UPDATE feedback_records SET status=?,resolution=?,resolved_at=NOW(6),handled_by_admin_id=? WHERE id=?",
                status, result, admin.getId(), id
            )!=1) throw BusinessException.notFound("记录不存在");
            Long userId = ((Number) row.get("user_id")).longValue();
            users.findById(userId).ifPresent(user -> notifications.send(
                user,
                "投诉反馈已处理",
                result,
                "/profile/feedback"
            ));
            realtime.afterCommit(userId, "FEEDBACK_RESOLVED", Map.of("feedbackId", id, "status", status));
            audit(admin,"UPDATE_FEEDBACK","FEEDBACK",id,status+":"+result,ip);
            return;
        }
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
    private String cleanDescription(String value){if(value==null||value.isBlank())return null;String result=sensitiveWords.mask(value.trim());if(result.length()>240)throw BusinessException.badRequest("介绍不能超过240个字");return result;}
    private String required(String value,String message){if(value==null||value.isBlank()) throw BusinessException.badRequest(message);return sensitiveWords.mask(value.trim());}
    private long count(String table,String where){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+where,Long.class);}
    private void audit(AdminUser admin,String action,String type,Object id,String detail,String ip){ AdminAuditLog l=new AdminAuditLog();l.setAdminUser(admin);l.setAction(action);l.setTargetType(type);l.setTargetId(String.valueOf(id));l.setDetail(detail);l.setIpAddress(ip);audits.save(l); }
    private TableSpec spec(String type){return switch(type){
        case "certifications" -> new TableSpec(
            "certifications",
            "certifications t JOIN users u ON u.id=t.user_id",
            "SELECT t.id,t.category,t.certification_type AS type,t.title,t.description,t.experience_location AS experienceLocation,t.experience_start_date AS experienceStartDate,t.experience_end_date AS experienceEndDate,t.experience_count AS experienceCount,t.experience_role AS experienceRole,t.experience_age_range AS experienceAgeRange,t.experience_education AS experienceEducation,t.experience_job AS experienceJob,t.status,t.enabled,t.rejection_reason AS rejectionReason,t.submitted_at AS submittedAt,t.privacy_confirmed_at AS privacyConfirmedAt,t.media_processing_status AS mediaProcessingStatus,t.media_processing_error AS mediaProcessingError,t.media_processed_at AS mediaProcessedAt,t.material_support_score AS materialSupportScore,t.common_relevance_score AS commonRelevanceScore,t.learnability_score AS learnabilityScore,t.clarity_score AS clarityScore,t.logic_consistency_score AS logicConsistencyScore,t.information_specificity_score AS informationSpecificityScore,t.reference_index AS referenceIndex,u.uid,u.nickname,(u.account_type='TEST') AS testData FROM certifications t JOIN users u ON u.id=t.user_id",
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
            "SELECT t.id,t.amount,t.payee_name_snapshot AS payeeName,t.alipay_account_masked_snapshot AS alipayAccount,t.risk_level AS riskLevel,t.risk_reasons AS riskReasons,t.status,t.batch_no AS batchNo,t.exported_at AS exportedAt,t.created_at AS createdAt,u.uid,u.nickname,(u.account_type='TEST') AS testData FROM withdrawals t JOIN users u ON u.id=t.user_id",
            List.of("u")
        );
        case "feedback" -> new TableSpec(
            "feedback_records",
            "feedback_records t JOIN users u ON u.id=t.user_id LEFT JOIN users tu ON tu.id=t.target_user_id",
            "SELECT t.id,t.inquiry_id AS inquiryId,t.feedback_type AS type,t.category,t.content,t.status,t.resolution,t.resolved_at AS resolvedAt,a.display_name AS handledBy,t.created_at AS createdAt,u.uid,u.nickname,tu.uid AS targetUid,(u.account_type='TEST') AS testData FROM feedback_records t JOIN users u ON u.id=t.user_id LEFT JOIN users tu ON tu.id=t.target_user_id LEFT JOIN admin_users a ON a.id=t.handled_by_admin_id",
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

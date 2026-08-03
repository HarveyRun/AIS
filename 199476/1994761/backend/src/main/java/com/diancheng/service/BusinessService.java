package com.diancheng.service;

import com.diancheng.api.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessService {
    private static final Set<String> IDEA_STATUSES = Set.of("待评估", "待付款", "排队中", "制作中", "已完成", "不制作");
    private static final Set<String> TEAM_STATUSES = Set.of("待审核", "沟通中", "已通过", "未通过");
    private static final Set<String> FEEDBACK_CATEGORIES = Set.of("功能问题", "使用问题", "建议");
    private static final Pattern MULTIPLE_IDEA = Pattern.compile("(并且|同时|以及|另外|还要|顺便)|[。；;！？!?].+");
    private final JdbcTemplate jdbc;
    private final AuthService auth;
    private final ObjectMapper objectMapper;

    public BusinessService(JdbcTemplate jdbc, AuthService auth, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> addIdea(String email, String type, String textInput, String parentId, boolean isPublic) {
        String text = trim(textInput);
        if (!Set.of("new", "iteration").contains(type)) bad("无效的提交类型。");
        int minimum = "new".equals(type) ? 10 : 6;
        if (text.length() < minimum || text.length() > 80 || MULTIPLE_IDEA.matcher(stripLastPunctuation(text)).find()) bad("想法内容不符合提交规则。");
        if ("iteration".equals(type)) {
            if (parentId == null || !exists("SELECT COUNT(*) FROM ideas WHERE id=? AND owner_email=? AND type='new'", parentId, email))
                bad("请选择有效的原想法。");
            isPublic = false;
        } else parentId = null;
        String id = id("idea");
        Instant now = Instant.now();
        jdbc.update("INSERT INTO ideas(id,owner_email,type,parent_id,text,status,level_value,fee,paid,decision,is_public,created_at) VALUES(?,?,?,?,?,'待评估',NULL,0,false,NULL,?,?)",
                id, email, type, parentId, text, isPublic, Timestamp.from(now));
        Map<String, Object> idea = new LinkedHashMap<>();
        idea.put("id", id); idea.put("type", type); idea.put("parentId", parentId); idea.put("text", text);
        idea.put("status", "待评估"); idea.put("level", null); idea.put("fee", 0); idea.put("paid", false);
        idea.put("isPublic", isPublic); idea.put("likedBy", List.of()); idea.put("createdAt", now.toString());
        return idea;
    }

    @Transactional
    public void toggleVisibility(String email, String ideaId) {
        int changed = jdbc.update("UPDATE ideas SET is_public=NOT is_public,updated_at=NOW(3) WHERE id=? AND owner_email=? AND type='new'", ideaId, email);
        if (changed == 0) notFound("想法不存在。");
    }

    @Transactional
    public boolean toggleLike(String viewer, String ideaId) {
        if (jdbc.queryForList("SELECT id FROM ideas WHERE id=? AND type='new' AND is_public=true FOR UPDATE", ideaId).isEmpty())
            notFound("想法不存在或未公开。");
        int removed = jdbc.update("DELETE FROM idea_likes WHERE idea_id=? AND user_email=?", ideaId, viewer);
        if (removed > 0) return false;
        jdbc.update("INSERT INTO idea_likes(idea_id,user_email,created_at) VALUES(?,?,NOW(3))", ideaId, viewer);
        return true;
    }

    @Transactional
    public Map<String, Object> payIdea(String email, String ideaId) {
        var ideas = jdbc.queryForList("SELECT fee,status FROM ideas WHERE id=? AND owner_email=? FOR UPDATE", ideaId, email);
        if (ideas.isEmpty() || !"待付款".equals(ideas.get(0).get("status"))) return fail("当前记录无需付款。");
        BigDecimal fee = money(ideas.get(0).get("fee"));
        BigDecimal balance = balanceForUpdate(email);
        if (balance.compareTo(fee) < 0) return fail("余额不足。");
        jdbc.update("UPDATE users SET balance=balance-? WHERE email=?", fee, email);
        jdbc.update("UPDATE ideas SET paid=true,status='排队中',updated_at=NOW(3) WHERE id=?", ideaId);
        transaction(email, "debit", "想法费用", fee, "IDEA", null, null, null);
        notify(email, "idea", "想法已进入队列", "想法费用已支付，已经进入制作队列。", "/center/ideas", ideaId, "idea-" + ideaId + "-排队中");
        return ok();
    }

    @Transactional
    public Map<String, Object> evaluateIdea(String admin, String owner, String ideaId, int level, String decision, BigDecimal fee) {
        if (level < 1 || level > 6) return fail("评级必须是 1—6 级。");
        if (!Set.of("制作", "不制作").contains(decision)) return fail("请选择有效的评估决定。");
        fee = fee == null ? BigDecimal.ZERO : fee.setScale(2, RoundingMode.HALF_UP);
        if ("制作".equals(decision) && level >= 4 && fee.compareTo(BigDecimal.ZERO) <= 0) return fail("4—6 级需要填写费用。");
        var ideaRows = jdbc.queryForList("SELECT status FROM ideas WHERE id=? AND owner_email=? FOR UPDATE", ideaId, owner);
        if (ideaRows.isEmpty()) return fail("想法不存在。");
        if (!"待评估".equals(ideaRows.get(0).get("status"))) return fail("该想法已经完成评估，不能重复评估。");
        String status;
        boolean paid;
        if ("不制作".equals(decision)) { status = "不制作"; fee = BigDecimal.ZERO; paid = false; }
        else if (level <= 3) { status = "排队中"; fee = BigDecimal.ZERO; paid = true; }
        else { status = "待付款"; paid = false; }
        jdbc.update("UPDATE ideas SET level_value=?,decision=?,fee=?,paid=?,status=?,reviewed_at=NOW(3),updated_at=NOW(3) WHERE id=? AND owner_email=?",
                level, decision, fee, paid, status, ideaId, owner);
        ideaNotification(owner, ideaId, status, fee);
        audit("更新想法状态", status + " · " + ideaId, admin, ideaId);
        return ok();
    }

    @Transactional
    public void updateIdeaStatus(String admin, String owner, String ideaId, String status) {
        if (!IDEA_STATUSES.contains(status)) bad("无效的想法状态。");
        var rows = jdbc.queryForList("SELECT status,paid FROM ideas WHERE id=? AND owner_email=? FOR UPDATE", ideaId, owner);
        if (rows.isEmpty()) notFound("想法不存在。");
        String current = String.valueOf(rows.get(0).get("status"));
        boolean allowed = ("待付款".equals(current) && "不制作".equals(status))
                || ("排队中".equals(current) && "制作中".equals(status))
                || ("制作中".equals(current) && "已完成".equals(status));
        if (!allowed) bad("当前状态不能变更为“" + status + "”。");
        jdbc.update("UPDATE ideas SET status=?,updated_at=NOW(3) WHERE id=? AND owner_email=?", status, ideaId, owner);
        BigDecimal fee = jdbc.queryForObject("SELECT fee FROM ideas WHERE id=?", BigDecimal.class, ideaId);
        ideaNotification(owner, ideaId, status, fee == null ? BigDecimal.ZERO : fee);
        audit("更新想法状态", status + " · " + ideaId, admin, ideaId);
    }

    @Transactional
    public void recharge(String email, BigDecimal amount) {
        amount = scaled(amount);
        if (amount.compareTo(BigDecimal.ONE) < 0 || amount.compareTo(new BigDecimal("99999")) > 0) bad("充值金额应在 1—99999 元之间。");
        jdbc.update("UPDATE users SET balance=balance+? WHERE email=?", amount, email);
        transaction(email, "credit", "支付宝充值", amount, null, null, null, null);
    }

    @Transactional
    public Map<String, Object> purchasePackage(String email, String packageId) {
        PackageDefinition definition = packageDefinition(packageId);
        if (definition == null) return fail("套餐不存在。");
        BigDecimal balance = balanceForUpdate(email);
        if (balance.compareTo(definition.price()) < 0)
            return fail("余额不足，还差 ¥" + definition.price().subtract(balance).setScale(2) + "。");
        Instant now = Instant.now();
        var activeRows = jdbc.queryForList("SELECT * FROM active_packages WHERE user_email=? FOR UPDATE", email);
        boolean sameActive = !activeRows.isEmpty() && packageId.equals(activeRows.get(0).get("package_id"))
                && ((Timestamp) activeRows.get(0).get("expires_at")).toInstant().isAfter(now);
        Instant base = sameActive ? ((Timestamp) activeRows.get(0).get("expires_at")).toInstant() : now;
        Instant expiresAt = base.plus(30, ChronoUnit.DAYS);
        int projectQuota = definition.projects() + (sameActive ? ((Number) activeRows.get(0).get("project_quota")).intValue() : 0);
        int iterationQuota = definition.iterations() + (sameActive ? ((Number) activeRows.get(0).get("iteration_quota")).intValue() : 0);
        String orderId = id("pkg");
        String benefits;
        try { benefits = objectMapper.writeValueAsString(definition.benefits()); } catch (Exception error) { throw new IllegalStateException(error); }
        jdbc.update("UPDATE users SET balance=balance-? WHERE email=?", definition.price(), email);
        jdbc.update("INSERT INTO package_orders(id,user_email,package_id,package_name,level_range,amount,pay_type,status,duration_days,project_quota,iteration_quota,benefits,created_at,activated_at,expires_at) VALUES(?,?,?,?,?,?,'BALANCE','PAID',30,?,?,?,?,?,?)",
                orderId, email, packageId, definition.name(), definition.levelRange(), definition.price(), definition.projects(), definition.iterations(), benefits,
                Timestamp.from(now), Timestamp.from(now), Timestamp.from(expiresAt));
        jdbc.update("INSERT INTO active_packages(user_email,package_id,package_name,level_range,started_at,expires_at,project_quota,iteration_quota,order_id) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE package_id=VALUES(package_id),package_name=VALUES(package_name),level_range=VALUES(level_range),started_at=VALUES(started_at),expires_at=VALUES(expires_at),project_quota=VALUES(project_quota),iteration_quota=VALUES(iteration_quota),order_id=VALUES(order_id)",
                email, packageId, definition.name(), definition.levelRange(), Timestamp.from(now), Timestamp.from(expiresAt), projectQuota, iterationQuota, orderId);
        transaction(email, "debit", "套餐开通 · " + definition.name(), definition.price(), "PACKAGE", orderId, null, "BALANCE");
        notify(email, "package", "套餐已生效", definition.name() + "已开通。", "/center/packages", orderId, "package-order-" + orderId);
        audit("开通或续费套餐", definition.name() + " · ¥" + definition.price(), email, orderId);
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("id", orderId); order.put("packageId", packageId); order.put("packageName", definition.name()); order.put("levelRange", definition.levelRange());
        order.put("amount", definition.price()); order.put("payType", "BALANCE"); order.put("status", "PAID"); order.put("durationDays", 30);
        order.put("projectQuota", definition.projects()); order.put("iterationQuota", definition.iterations()); order.put("benefits", definition.benefits());
        order.put("createdAt", now.toString()); order.put("activatedAt", now.toString()); order.put("expiresAt", expiresAt.toString());
        return Map.of("ok", true, "order", order);
    }

    public void updateProfile(String email, String nameInput) {
        String name = trim(nameInput);
        if (name.length() > 30) bad("显示名称不能超过 30 个字。");
        jdbc.update("UPDATE users SET name=? WHERE email=?", name, email);
    }

    @Transactional
    public Map<String, Object> changePassword(String email, String current, String next, String currentToken) {
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email=?", String.class, email);
        if (!auth.encoder().matches(current == null ? "" : current, hash)) return fail("当前密码不正确。");
        if (next == null || next.length() < 8 || next.length() > 72) return fail("新密码长度应为 8—72 位。");
        jdbc.update("UPDATE users SET password_hash=? WHERE email=?", auth.encoder().encode(next), email);
        jdbc.update("DELETE FROM auth_sessions WHERE user_email=? AND token<>?", email, currentToken);
        return ok();
    }

    public void deleteAccount(String email) {
        if (AuthService.ADMIN_EMAIL.equals(email)) bad("管理员账号不能通过前台删除。");
        jdbc.update("DELETE FROM users WHERE email=?", email);
    }

    @Transactional
    public Map<String, Object> createFeedback(String email, String contentInput, String page, String category) {
        String content = trim(contentInput);
        if (content.length() < 5 || content.length() > 500) bad("反馈内容应为 5—500 个字。");
        if (!FEEDBACK_CATEGORIES.contains(category)) category = "使用问题";
        String id = id("fb"), messageId = id("msg"); Instant now = Instant.now();
        jdbc.update("INSERT INTO feedbacks(id,user_email,page,category,status,created_at,updated_at) VALUES(?,?,?,?,'待管理员回复',?,?)",
                id, email, page == null || page.isBlank() ? "index.html" : page, category, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("INSERT INTO feedback_messages(id,feedback_id,role,email,content,created_at) VALUES(?,?,'user',?,?,?)", messageId, id, email, content, Timestamp.from(now));
        notify(AuthService.ADMIN_EMAIL, "feedback", "收到新的用户反馈", category + " · " + shorten(content, 60), "/center/admin", id, "feedback-new-" + id);
        audit("提交反馈", category + " · " + shorten(content, 80), email == null ? "游客" : email, id);
        return Map.of("id", id, "userEmail", email == null ? "" : email, "page", page, "category", category, "status", "待管理员回复", "createdAt", now.toString(), "updatedAt", now.toString(),
                "messages", List.of(Map.of("id", messageId, "role", "user", "email", email == null ? "" : email, "content", content, "createdAt", now.toString())));
    }

    @Transactional
    public Map<String, Object> appendFeedback(String feedbackId, String role, String actor, String contentInput) {
        String content = trim(contentInput);
        if (!Set.of("user", "admin").contains(role)) bad("无效的回复身份。");
        if (content.length() < 2 || content.length() > 500) bad("回复内容应为 2—500 个字。");
        var rows = jdbc.queryForList("SELECT user_email,status FROM feedbacks WHERE id=? FOR UPDATE", feedbackId);
        if (rows.isEmpty()) notFound("反馈不存在。");
        if ("已结束".equals(rows.get(0).get("status"))) bad("反馈会话已经结束。");
        String owner = (String) rows.get(0).get("user_email");
        if ("admin".equals(role) && !AuthService.ADMIN_EMAIL.equals(actor)) forbidden();
        if ("user".equals(role) && (owner == null || !owner.equals(actor))) forbidden();
        String id = id("msg"); Instant now = Instant.now(); String status = "admin".equals(role) ? "待用户回复" : "待管理员回复";
        jdbc.update("INSERT INTO feedback_messages(id,feedback_id,role,email,content,created_at) VALUES(?,?,?,?,?,?)", id, feedbackId, role, actor, content, Timestamp.from(now));
        jdbc.update("UPDATE feedbacks SET status=?,updated_at=? WHERE id=?", status, Timestamp.from(now), feedbackId);
        String recipient = "admin".equals(role) ? owner : AuthService.ADMIN_EMAIL;
        if (recipient != null) notify(recipient, "feedback", "admin".equals(role) ? "你的反馈收到回复" : "用户追加了反馈回复", shorten(content, 80), "/center/notifications", feedbackId, "feedback-message-" + id);
        audit("admin".equals(role) ? "管理员回复反馈" : "用户回复反馈", shorten(content, 100), actor, feedbackId);
        return Map.of("id", id, "role", role, "email", actor, "content", content, "createdAt", now.toString());
    }

    @Transactional
    public void closeFeedback(String feedbackId, String actor) {
        var rows = jdbc.queryForList("SELECT user_email,category FROM feedbacks WHERE id=?", feedbackId);
        if (rows.isEmpty()) return;
        String owner = (String) rows.get(0).get("user_email");
        if (!AuthService.ADMIN_EMAIL.equals(actor) && !actor.equals(owner)) forbidden();
        jdbc.update("UPDATE feedbacks SET status='已结束',updated_at=NOW(3) WHERE id=?", feedbackId);
        audit("结束反馈会话", String.valueOf(rows.get(0).get("category")), actor, feedbackId);
        if (owner != null) notify(owner, "feedback", "反馈处理已结束", "反馈会话已结束。", "/center/notifications", feedbackId, "feedback-closed-" + feedbackId);
    }

    public void markNotification(String email, String id) { jdbc.update("UPDATE notifications SET is_read=true WHERE id=? AND user_email=?", id, email); }
    public void markAllNotifications(String email) { jdbc.update("UPDATE notifications SET is_read=true WHERE user_email=?", email); }
    public void markBusinessNotifications(String email, String businessId) { jdbc.update("UPDATE notifications SET is_read=true WHERE user_email=? AND business_id=?", email, businessId); }

    @Transactional
    public void ensureDerivedNotifications(String email) {
        jdbc.queryForList("SELECT id,status,fee,text FROM ideas WHERE owner_email=?", email).forEach(row ->
                ideaNotification(email, String.valueOf(row.get("id")), String.valueOf(row.get("status")), money(row.get("fee"))));
        var active = jdbc.queryForList("SELECT * FROM active_packages WHERE user_email=?", email);
        if (active.isEmpty()) return;
        var row = active.get(0); long days = ChronoUnit.DAYS.between(Instant.now(), ((Timestamp) row.get("expires_at")).toInstant());
        String orderId = String.valueOf(row.get("order_id"));
        if (days >= 0 && days <= 7) notify(email, "package", "套餐即将到期", row.get("package_name") + "将在 " + days + " 天后到期。", "/center/packages", orderId, "package-expiry-" + orderId + "-" + days);
        if (days < 0) notify(email, "package", "套餐已到期", row.get("package_name") + "已到期，可重新开通。", "/center/packages", orderId, "package-expired-" + orderId);
        int projects = ((Number) row.get("project_quota")).intValue(), iterations = ((Number) row.get("iteration_quota")).intValue();
        if (projects <= 1 || iterations <= 2) notify(email, "package", "套餐权益即将用完", "剩余 " + projects + " 个项目、" + iterations + " 次迭代。", "/center/packages", orderId, "package-quota-" + orderId + "-" + projects + "-" + iterations);
    }

    @Transactional
    public void submitTeamApplication(String email, Map<String, Object> application) {
        if (application == null) {
            List<String> resumeIds = jdbc.query("SELECT resume_id FROM team_applications WHERE user_email=? AND resume_id IS NOT NULL",
                    (rs, row) -> rs.getString(1), email);
            jdbc.update("DELETE FROM team_applications WHERE user_email=?", email);
            resumeIds.forEach(resumeId -> jdbc.update("DELETE FROM stored_files WHERE id=? AND user_email=?", resumeId, email));
            return;
        }
        String skill = stringValue(application.get("skill")); String resumeId = stringValue(application.get("resumeId"));
        if (skill.length() < 2 || skill.length() > 60) bad("可以负责的内容应为 2—60 个字。");
        String intro = stringValue(application.get("intro")), time = stringValue(application.get("time"));
        if (intro.length() > 1000) bad("补充说明不能超过 1000 个字。");
        if (!Set.of("偶尔参与", "每周少量时间", "可以稳定参与").contains(time)) bad("请选择有效的可投入时间。");
        var files = jdbc.queryForList("SELECT file_name,size_bytes FROM stored_files WHERE id=? AND user_email=? AND kind='resume'", resumeId, email);
        if (files.isEmpty()) bad("没有找到已上传的简历，请重新上传。");
        String resumeName = String.valueOf(files.get(0).get("file_name"));
        long resumeSize = ((Number) files.get(0).get("size_bytes")).longValue();
        if (resumeId.isBlank() || resumeName.isBlank()) bad("必须上传简历。");
        if (resumeSize > 20L * 1024 * 1024) bad("简历不能超过 20 MB。");
        String status = "待审核";
        jdbc.update("INSERT INTO team_applications(user_email,skill,intro,available_time,resume_id,resume_name,resume_size,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,NOW(3),NULL) ON DUPLICATE KEY UPDATE skill=VALUES(skill),intro=VALUES(intro),available_time=VALUES(available_time),resume_id=VALUES(resume_id),resume_name=VALUES(resume_name),resume_size=VALUES(resume_size),status='待审核',updated_at=NOW(3)",
                email, skill, intro, time, resumeId, resumeName, resumeSize, status);
    }

    public void updateTeamStatus(String admin, String email, String status) {
        if (!TEAM_STATUSES.contains(status)) bad("无效的团队申请状态。");
        if (jdbc.update("UPDATE team_applications SET status=?,updated_at=NOW(3) WHERE user_email=?", status, email) == 0) notFound("团队申请不存在。");
        audit("审核团队申请", email + " · " + status, admin, email);
    }

    @Transactional
    public Map<String, Object> createDeposit(String email, BigDecimal amount) {
        amount = scaled(amount); if (amount.compareTo(new BigDecimal("2000.00")) != 0) return fail("商务合作押金固定为 ¥2000.00。");
        BigDecimal balance = balanceForUpdate(email);
        if (exists("SELECT COUNT(*) FROM cooperation_deposits WHERE user_email=? AND status NOT IN ('已退回','已取消')", email)) return ok();
        if (balance.compareTo(amount) < 0) return fail("余额不足。");
        String id = id("deposit"); jdbc.update("UPDATE users SET balance=balance-? WHERE email=?", amount, email);
        jdbc.update("INSERT INTO cooperation_deposits(id,user_email,amount,status,created_at,updated_at) VALUES(?,?,?,'已支付',NOW(3),NOW(3))", id, email, amount);
        transaction(email, "debit", "商务合作押金", amount, "COOPERATION_DEPOSIT", null, id, null);
        audit("支付商务合作押金", "¥" + amount, email, id);
        notify(email, "cooperation", "商务合作押金已支付", "合作联系人会根据提交顺序与你沟通，押金将在沟通结束后退回。", "/center/notifications", id, "deposit-paid-" + id);
        return ok();
    }

    @Transactional
    public void updateDeposit(String admin, String depositId, String status) {
        if (!Set.of("已联系", "已退回").contains(status)) bad("无效的押金状态。");
        var rows = jdbc.queryForList("SELECT * FROM cooperation_deposits WHERE id=? FOR UPDATE", depositId); if (rows.isEmpty()) notFound("押金记录不存在。");
        var row = rows.get(0); if ("已退回".equals(row.get("status"))) return; String email = String.valueOf(row.get("user_email")); BigDecimal amount = money(row.get("amount"));
        if ("已退回".equals(status)) {
            jdbc.update("UPDATE users SET balance=balance+? WHERE email=?", amount, email);
            transaction(email, "credit", "商务合作押金退回", amount, "COOPERATION_REFUND", null, depositId, null);
            jdbc.update("UPDATE cooperation_deposits SET status=?,updated_at=NOW(3),refunded_at=NOW(3) WHERE id=?", status, depositId);
        } else jdbc.update("UPDATE cooperation_deposits SET status=?,updated_at=NOW(3) WHERE id=?", status, depositId);
        audit("已退回".equals(status) ? "退回商务合作押金" : "更新商务合作状态", status, admin, depositId);
        notify(email, "cooperation", "已退回".equals(status) ? "商务合作押金已退回" : "商务合作进度更新", "已退回".equals(status) ? "¥" + amount + " 已退回账户余额。" : "当前状态：" + status, "/center/notifications", depositId, "deposit-" + depositId + "-" + status);
    }

    private void ideaNotification(String owner, String ideaId, String status, BigDecimal fee) {
        String title = null, content = null, type = "idea";
        if ("待付款".equals(status)) { title = "想法等待付款"; content = "需要支付 ¥" + fee.setScale(2) + " 后进入制作。"; type = "payment"; }
        else if ("排队中".equals(status)) { title = "想法已进入队列"; content = "想法已经进入制作队列。"; }
        else if ("制作中".equals(status)) { title = "想法正在制作"; content = "想法已经开始制作。"; }
        else if ("已完成".equals(status)) { title = "想法已经完成"; content = "想法已经完成，可以前往个人中心查看。"; }
        else if ("不制作".equals(status)) { title = "想法评估完成"; content = "该想法本次暂不进入制作。"; }
        if (title != null) notify(owner, type, title, content, "/center/ideas", ideaId, "idea-" + ideaId + "-" + status);
    }

    private void notify(String email, String type, String title, String content, String link, String businessId, String dedupeKey) {
        if (email == null || !exists("SELECT COUNT(*) FROM users WHERE email=?", email)) return;
        jdbc.update("INSERT IGNORE INTO notifications(id,user_email,type,title,content,link,business_id,dedupe_key,is_read,created_at) VALUES(?,?,?,?,?,?,?,?,false,NOW(3))",
                id("notice"), email, type, title, content, link, businessId, dedupeKey);
    }

    private void audit(String action, String detail, String actor, String target) {
        jdbc.update("INSERT INTO audit_logs(id,action,detail,actor,target_id,created_at) VALUES(?,?,?,?,?,NOW(3))", id("audit"), action, detail, actor == null ? "系统" : actor, target);
        jdbc.update("DELETE FROM audit_logs WHERE id NOT IN (SELECT id FROM (SELECT id FROM audit_logs ORDER BY created_at DESC LIMIT 500) recent)");
    }

    private void transaction(String email, String type, String title, BigDecimal amount, String businessType, String orderId, String depositId, String payType) {
        jdbc.update("INSERT INTO transactions(id,user_email,type,title,amount,business_type,order_id,deposit_id,pay_type,created_at) VALUES(?,?,?,?,?,?,?,?,?,NOW(3))",
                id("tx"), email, type, title, amount, businessType, orderId, depositId, payType);
    }

    private BigDecimal balanceForUpdate(String email) {
        return jdbc.queryForObject("SELECT balance FROM users WHERE email=? FOR UPDATE", BigDecimal.class, email);
    }

    private boolean exists(String sql, Object... values) { Integer count = jdbc.queryForObject(sql, Integer.class, values); return count != null && count > 0; }
    private static String id(String prefix) { return prefix + "-" + UUID.randomUUID().toString().replace("-", ""); }
    private static BigDecimal scaled(BigDecimal value) { if (value == null) bad("金额无效。"); return value.setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal money(Object value) { return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value)); }
    private static String trim(String value) { return value == null ? "" : value.trim(); }
    private static String stringValue(Object value) { return value instanceof String text ? text.trim() : ""; }
    private static String shorten(String value, int size) { return value.length() <= size ? value : value.substring(0, size); }
    private static String stripLastPunctuation(String value) { return value.replaceFirst("[。；;！？!?]$", ""); }
    private static Map<String, Object> ok() { return Map.of("ok", true); }
    private static Map<String, Object> fail(String error) { return Map.of("ok", false, "error", error); }
    private static void bad(String message) { throw new ApiException(HttpStatus.BAD_REQUEST, message); }
    private static void notFound(String message) { throw new ApiException(HttpStatus.NOT_FOUND, message); }
    private static void forbidden() { throw new ApiException(HttpStatus.FORBIDDEN, "没有权限执行该操作。"); }

    private PackageDefinition packageDefinition(String id) {
        if ("standard".equals(id)) return new PackageDefinition("标准套餐", "LEVEL 1—3", new BigDecimal("25"), 2, 4,
                List.of("每月 2 个微型项目", "每月 4 次迭代调整", "支持完整源码交付", "不支持模块概述梳理、细节定制", "不支持对接第三方与需求方", "最终成品交付标准由平台审核确认"));
        if ("upgrade".equals(id)) return new PackageDefinition("升级套餐", "LEVEL 4—6", new BigDecimal("99"), 2, 15,
                List.of("每月 2 个小型项目", "每月 15 次迭代调整", "支持完整源码交付", "仅支持模块概述梳理，不支持细节定制", "不支持对接第三方与需求方", "最终成品交付标准由平台审核确认"));
        return null;
    }

    private record PackageDefinition(String name, String levelRange, BigDecimal price, int projects, int iterations, List<String> benefits) {}
}

package com.diancheng.config;

import com.diancheng.service.AuthService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final AuthService auth;

    public DataSeeder(JdbcTemplate jdbc, AuthService auth) { this.jdbc = jdbc; this.auth = auth; }

    @Override
    @Transactional
    public void run(String... args) {
        Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (users != null && users > 0) return;
        Instant now = Instant.now();
        seedUser(AuthService.ADMIN_EMAIL, "timeline.1994.1976", "DC-128407", "DC-199476", new BigDecimal("3688"), true, now.minus(180, ChronoUnit.DAYS));
        List<String> names = List.of("林一", "周舟", "陈默", "小满", "阿禾", "木子");
        for (int index = 0; index < names.size(); index++) {
            String email = "demo%02d@diancheng.test".formatted(index + 1);
            seedUser(email, names.get(index), "DC-%06d".formatted(410001 + index), "DC-128407", new BigDecimal("3688"), false, now.minus(90L - index, ChronoUnit.DAYS));
            seedIdeas(email, names.get(index), index, now);
        }
        seedIdeas(AuthService.ADMIN_EMAIL, "管理员", 7, now);
        seedFeedback(now);
        seedTeam(now);
        seedDeposit(now);
        jdbc.update("INSERT INTO transactions(id,user_email,type,title,amount,created_at) VALUES(?,?,'credit','初始演示余额',3688,?)",
                id("tx"), "demo01@diancheng.test", Timestamp.from(now.minus(30, ChronoUnit.DAYS)));
    }

    private void seedUser(String email, String name, String invite, String usedInvite, BigDecimal balance, boolean admin, Instant created) {
        jdbc.update("INSERT INTO users(email,name,password_hash,invite_code,used_invite_code,balance,is_admin,created_at) VALUES(?,?,?,?,?,?,?,?)",
                email, name, auth.encoder().encode("12345678"), invite, usedInvite, balance, admin, Timestamp.from(created));
    }

    private void seedIdeas(String email, String name, int offset, Instant now) {
        String[] texts = {
                "做一个可以记录每天心情和一句话的小网站",
                "做一个能给家人分享旅行照片的私人相册",
                "做一个按季节推荐家常菜的简单工具",
                "做一个帮助自由职业者记录项目进度的页面",
                "做一个可以整理读书摘录和感想的小站",
                "做一个用于展示手工作品的个人橱窗",
                "做一个记录孩子成长瞬间的家庭时间轴",
                "做一个能快速生成活动报名页的工具"
        };
        String ideaId = id("idea");
        String status = offset % 4 == 0 ? "待评估" : offset % 4 == 1 ? "排队中" : offset % 4 == 2 ? "制作中" : "已完成";
        Instant created = now.minus(20L - offset, ChronoUnit.DAYS);
        jdbc.update("INSERT INTO ideas(id,owner_email,type,parent_id,text,status,level_value,fee,paid,decision,is_public,created_at,updated_at) VALUES(?,?,'new',NULL,?,?,?,?,?,'制作',true,?,?)",
                ideaId, email, texts[offset % texts.length], status, status.equals("待评估") ? null : 2, BigDecimal.ZERO, !status.equals("待评估"), Timestamp.from(created), Timestamp.from(created.plus(1, ChronoUnit.DAYS)));
        if (offset < 5) {
            jdbc.update("INSERT INTO idea_likes(idea_id,user_email,created_at) VALUES(?,?,?)", ideaId, AuthService.ADMIN_EMAIL, Timestamp.from(created.plus(2, ChronoUnit.DAYS)));
            jdbc.update("INSERT INTO idea_likes(idea_id,user_email,created_at) VALUES(?,?,?)", ideaId, "demo01@diancheng.test", Timestamp.from(created.plus(3, ChronoUnit.DAYS)));
        }
        if (offset % 2 == 0) {
            jdbc.update("INSERT INTO ideas(id,owner_email,type,parent_id,text,status,level_value,fee,paid,decision,is_public,created_at) VALUES(?,?,'iteration',?,?,'待评估',NULL,0,false,NULL,false,?)",
                    id("idea"), email, ideaId, "增加一个更清楚的分类筛选入口", Timestamp.from(created.plus(5, ChronoUnit.DAYS)));
        }
    }

    private void seedFeedback(Instant now) {
        String feedbackId = id("fb");
        jdbc.update("INSERT INTO feedbacks(id,user_email,page,category,status,created_at,updated_at) VALUES(?,?,'ideas.html','使用问题','待用户回复',?,?)",
                feedbackId, "demo01@diancheng.test", Timestamp.from(now.minus(8, ChronoUnit.DAYS)), Timestamp.from(now.minus(7, ChronoUnit.DAYS)));
        jdbc.update("INSERT INTO feedback_messages(id,feedback_id,role,email,content,created_at) VALUES(?,?,'user',?,'点赞以后数字没有及时变化。',?)",
                id("msg"), feedbackId, "demo01@diancheng.test", Timestamp.from(now.minus(8, ChronoUnit.DAYS)));
        jdbc.update("INSERT INTO feedback_messages(id,feedback_id,role,email,content,created_at) VALUES(?,?,'admin',?,'已经修复，请刷新页面后再试。',?)",
                id("msg"), feedbackId, AuthService.ADMIN_EMAIL, Timestamp.from(now.minus(7, ChronoUnit.DAYS)));
    }

    private void seedTeam(Instant now) {
        jdbc.update("INSERT INTO team_applications(user_email,skill,intro,available_time,resume_id,resume_name,resume_size,status,created_at) VALUES(?,?,?,?,?,?,?,'待审核',?)",
                "demo02@diancheng.test", "前端开发", "熟悉 React 与交互实现", "每周少量时间", "resume-demo02", "示例简历.pdf", 128000,
                Timestamp.from(now.minus(6, ChronoUnit.DAYS)));
    }

    private void seedDeposit(Instant now) {
        jdbc.update("INSERT INTO cooperation_deposits(id,user_email,amount,status,created_at,updated_at) VALUES(?,?,2000,'已支付',?,?)",
                id("deposit"), "demo03@diancheng.test", Timestamp.from(now.minus(3, ChronoUnit.DAYS)), Timestamp.from(now.minus(3, ChronoUnit.DAYS)));
    }

    private static String id(String prefix) { return prefix + "-" + UUID.randomUUID().toString().replace("-", ""); }
}

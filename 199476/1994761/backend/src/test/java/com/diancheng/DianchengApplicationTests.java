package com.diancheng;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diancheng.api.ApiException;
import com.diancheng.service.AuthService;
import com.diancheng.service.BusinessService;
import com.diancheng.service.SnapshotService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class DianchengApplicationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired SnapshotService snapshots;
    @Autowired AuthService auth;
    @Autowired BusinessService business;
    @Autowired MockMvc mockMvc;

    @Test
    void schemaAndSeedDataAreAvailable() {
        Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(users).isNotNull().isGreaterThanOrEqualTo(2);
    }

    @Test
    void publicSnapshotContainsOnlyPublicBusinessData() {
        Map<String, Object> snapshot = snapshots.snapshot(null);
        assertThat(snapshot).containsKeys("users", "feedbacks", "notifications", "auditLogs", "cooperationDeposits");
        assertThat((Iterable<?>) snapshot.get("feedbacks")).isEmpty();
        assertThat((Iterable<?>) snapshot.get("auditLogs")).isEmpty();

        Map<String, Map<String, Object>> users = (Map<String, Map<String, Object>>) snapshot.get("users");
        assertThat(users.keySet()).allMatch(key -> key.startsWith("public-") && !key.contains("@"));
        users.values().forEach(user -> {
            assertThat(user.get("email")).isEqualTo("");
            assertThat(user.get("inviteCode")).isEqualTo("");
            ((Iterable<Map<String, Object>>) user.get("ideas")).forEach(idea -> {
                assertThat(idea.get("type")).isEqualTo("new");
                assertThat(idea.get("isPublic")).isEqualTo(true);
                assertThat((Iterable<String>) idea.get("likedBy")).allMatch(value -> !value.contains("@"));
            });
        });
    }

    @Test
    @Transactional
    void passwordChangeKeepsCurrentSessionAndInvalidatesOtherSessions() {
        String email = testEmail();
        Map<String, Object> registration = auth.register(email, "old-pass-123", "old-pass-123", "199476");
        String currentToken = String.valueOf(registration.get("token"));
        jdbc.update("INSERT INTO auth_sessions(token,user_email,created_at,expires_at) VALUES('other-session',?,NOW(3),DATE_ADD(NOW(3), INTERVAL 1 DAY))", email);

        Map<String, Object> result = business.changePassword(email, "old-pass-123", "new-pass-456", currentToken);

        assertThat(result.get("ok")).isEqualTo(true);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE token=?", Integer.class, currentToken)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE token='other-session'", Integer.class)).isZero();
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email=?", String.class, email);
        assertThat(auth.encoder().matches("new-pass-456", hash)).isTrue();
    }

    @Test
    @Transactional
    void ideaEvaluationAndProductionStatusAreSingleDirection() {
        String email = testEmail();
        auth.register(email, "idea-pass-123", "idea-pass-123", "199476");
        String ideaId = String.valueOf(business.addIdea(email, "new", "做一个只记录每日饮水量的小工具", null, true).get("id"));

        assertThat(business.evaluateIdea(AuthService.ADMIN_EMAIL, email, ideaId, 2, "制作", BigDecimal.ZERO).get("ok")).isEqualTo(true);
        assertThat(business.evaluateIdea(AuthService.ADMIN_EMAIL, email, ideaId, 2, "制作", BigDecimal.ZERO).get("ok")).isEqualTo(false);
        assertThatThrownBy(() -> business.updateIdeaStatus(AuthService.ADMIN_EMAIL, email, ideaId, "已完成"))
                .isInstanceOf(ApiException.class);

        business.updateIdeaStatus(AuthService.ADMIN_EMAIL, email, ideaId, "制作中");
        business.updateIdeaStatus(AuthService.ADMIN_EMAIL, email, ideaId, "已完成");
        assertThat(jdbc.queryForObject("SELECT status FROM ideas WHERE id=?", String.class, ideaId)).isEqualTo("已完成");
    }

    @Test
    void cooperationDepositRejectsClientControlledAmounts() {
        Map<String, Object> result = business.createDeposit("demo01@diancheng.test", new BigDecimal("1.00"));
        assertThat(result.get("ok")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("商务合作押金固定为 ¥2000.00。");
    }

    @Test
    @Transactional
    void resumeUploadApplicationAndWithdrawalAreOneCompleteFlow() throws Exception {
        String email = testEmail();
        Map<String, Object> registration = auth.register(email, "file-pass-123", "file-pass-123", "199476");
        String token = String.valueOf(registration.get("token"));
        String fileId = "resume-" + UUID.randomUUID().toString().replace("-", "");
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.4\n%%EOF".getBytes());

        mockMvc.perform(multipart("/api/files")
                        .file(file)
                        .param("id", fileId)
                        .param("kind", "resume")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/team/application")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skill":"视觉设计","time":"每周少量时间","resumeId":"%s","resumeName":"ignored.pdf","resumeSize":1,"status":"已通过"}
                                """.formatted(fileId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM team_applications WHERE user_email=?", String.class, email)).isEqualTo("待审核");
        assertThat(jdbc.queryForObject("SELECT resume_name FROM team_applications WHERE user_email=?", String.class, email)).isEqualTo("resume.pdf");

        mockMvc.perform(put("/api/team/application")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM team_applications WHERE user_email=?", Integer.class, email)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stored_files WHERE id=?", Integer.class, fileId)).isZero();
    }

    private static String testEmail() {
        return "integration-" + UUID.randomUUID() + "@diancheng.test";
    }
}

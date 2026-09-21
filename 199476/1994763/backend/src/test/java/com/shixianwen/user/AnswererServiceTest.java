package com.shixianwen.user;

import com.shixianwen.certification.CertificationPublicMediaService;
import com.shixianwen.certification.CertificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnswererServiceTest {
    @Test
    void homepageSearchOnlyReturnsUsersAcceptingNewInquiries() {
        UserRepository users = mock(UserRepository.class);
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setAccountType("NORMAL");
        when(users.findById(1L)).thenReturn(Optional.of(currentUser));
        CapturingJdbcTemplate jdbc = new CapturingJdbcTemplate();
        AnswererService service = new AnswererService(
            users,
            mock(CertificationRepository.class),
            jdbc,
            mock(CertificationPublicMediaService.class)
        );

        AnswererService.AnswererPage result = service.search(1L, "", 0, 10);

        assertEquals(List.of(), result.items());
        assertFalse(jdbc.sql.contains("experience_business_type"));
        assertFalse(jdbc.sql.contains("certification_type='IDENTITY'"));
        assertTrue(jdbc.sql.contains("accepting_inquiries=TRUE"));
    }

    @Test
    void homepageCanSortByReferenceIndexAscending() {
        CapturingJdbcTemplate jdbc = new CapturingJdbcTemplate();
        AnswererService service = service(jdbc);

        service.search(1L, "", "REFERENCE_INDEX", "ASC", 0, 20);

        assertTrue(jdbc.sql.contains(
            "ORDER BY ce.reference_index IS NULL ASC,ce.reference_index ASC,ce.id DESC"
        ));
    }

    @Test
    void homepageCanSortByLikeCountDescending() {
        CapturingJdbcTemplate jdbc = new CapturingJdbcTemplate();
        AnswererService service = service(jdbc);

        service.search(1L, "", "LIKE_COUNT", "DESC", 0, 20);

        assertTrue(jdbc.sql.contains("ORDER BY COALESCE(el.like_count,0) DESC,ce.id DESC"));
    }

    @Test
    void homepageRejectsUnrecognizedSortExpression() {
        CapturingJdbcTemplate jdbc = new CapturingJdbcTemplate();
        AnswererService service = service(jdbc);

        service.search(1L, "", "id DESC; DROP TABLE users", "ASC", 0, 20);

        assertTrue(jdbc.sql.contains("ORDER BY ce.id DESC"));
        assertFalse(jdbc.sql.contains("DROP TABLE"));
    }

    private AnswererService service(CapturingJdbcTemplate jdbc) {
        UserRepository users = mock(UserRepository.class);
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setAccountType("NORMAL");
        when(users.findById(1L)).thenReturn(Optional.of(currentUser));
        return new AnswererService(
            users,
            mock(CertificationRepository.class),
            jdbc,
            mock(CertificationPublicMediaService.class)
        );
    }

    private static final class CapturingJdbcTemplate extends JdbcTemplate {
        private String sql;

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.sql = sql;
            return List.of();
        }
    }
}

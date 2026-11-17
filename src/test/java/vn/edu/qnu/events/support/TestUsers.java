package vn.edu.qnu.events.support;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.UUID;

public final class TestUsers {

    private TestUsers() {
    }

    public static UUID createOrgClass(JdbcClient jdbcClient, String code, String name) {
        UUID id = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO org_class (id, code, name, faculty_name, cohort_year)
                VALUES (:id, :code, :name, 'Khoa CNTT', 46)
                """)
                .param("id", id)
                .param("code", code)
                .param("name", name)
                .update();
        return id;
    }

    public static UUID createUser(JdbcClient jdbcClient, String loginId, String role, UUID classId) {
        UUID id = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO app_user (id, login_id, password_hash, full_name, class_id)
                VALUES (:id, :loginId, '$2a$12$e8rG72M38K/f8l1h5VqEee3WvXQ69kK2mH8L2H.8B8x9o2K6i3zG', 'Test User', :classId)
                """)
                .param("id", id)
                .param("loginId", loginId)
                .param("classId", classId)
                .update();

        if (role != null) {
            jdbcClient.sql("""
                    INSERT INTO user_role (user_id, role)
                    VALUES (:userId, :role)
                    """)
                    .param("userId", id)
                    .param("role", role)
                    .update();
        }

        return id;
    }
}


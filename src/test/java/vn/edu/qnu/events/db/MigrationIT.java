package vn.edu.qnu.events.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class MigrationIT {

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void cleanUp() {
        jdbcClient.sql("DELETE FROM attendance_attempts").update();
        jdbcClient.sql("DELETE FROM attendances").update();
        jdbcClient.sql("DELETE FROM registrations").update();
        jdbcClient.sql("DELETE FROM event_audience").update();
        jdbcClient.sql("DELETE FROM events WHERE title = 'Test Event'").update();
        jdbcClient.sql("""
            DELETE FROM user_role
            WHERE user_id NOT IN (SELECT id FROM app_user WHERE login_id IN ('owner01', 'owner02', 'sv001', 'sv002', 'sv003', 'admin01'))
            AND user_id NOT IN (SELECT actor_id FROM audit_logs WHERE actor_id IS NOT NULL)
        """).update();
        jdbcClient.sql("""
            DELETE FROM auth_session
            WHERE user_id NOT IN (SELECT id FROM app_user WHERE login_id IN ('owner01', 'owner02', 'sv001', 'sv002', 'sv003', 'admin01'))
            AND user_id NOT IN (SELECT actor_id FROM audit_logs WHERE actor_id IS NOT NULL)
        """).update();
        jdbcClient.sql("""
            DELETE FROM app_user
            WHERE login_id NOT IN ('owner01', 'owner02', 'sv001', 'sv002', 'sv003', 'admin01')
            AND id NOT IN (SELECT actor_id FROM audit_logs WHERE actor_id IS NOT NULL)
        """).update();
        jdbcClient.sql("DELETE FROM org_class WHERE code NOT IN ('K49CNTT1', 'K49CNTT2')").update();
    }

    @Test
    @DisplayName("Tất cả 10 bảng của Sprint 1 phải tồn tại trong CSDL")
    void allTablesExist() {
        List<String> expectedTables = List.of(
                "org_class", "app_user", "user_role", "auth_session",
                "events", "event_audience", "registrations", "attendances",
                "attendance_attempts", "audit_logs"
        );

        List<String> actualTables = jdbcClient.sql("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                """)
                .query(String.class)
                .list();

        assertThat(actualTables).containsAll(expectedTables);
    }

    @Test
    @DisplayName("CHECK constraint từ chối sự kiện có quota_total = 0")
    void quotaCheckConstraintRejectsZero() {
        UUID ownerId = createTestUser("owner_quota_test");

        assertThatThrownBy(() -> jdbcClient.sql("""
                INSERT INTO events (
                    id, owner_id, title, description, location,
                    start_at, end_at, registration_deadline,
                    attendance_open_at, attendance_close_at,
                    quota_total, activity_type
                ) VALUES (
                    gen_random_uuid(), :ownerId, 'Test Event', 'Desc', 'Hall A',
                    now() + interval '1 day', now() + interval '2 days', now() + interval '1 day',
                    now() + interval '1 day', now() + interval '2 days',
                    0, 'WORKSHOP'
                )
                """)
                .param("ownerId", ownerId)
                .update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("events_quota_total_check");
    }

    @Test
    @DisplayName("Chặn 2 lượt đăng ký active trùng lặp cho cùng 1 sinh viên")
    void registrationsUniqueActiveConstraintBlocksDuplicate() {
        UUID ownerId = createTestUser("owner_reg_dup");
        UUID studentId = createTestUser("student_reg_dup");
        UUID eventId = createTestEvent(ownerId);

        // 1. Đăng ký lần đầu: REGISTERED
        jdbcClient.sql("""
                INSERT INTO registrations (event_id, student_id, status)
                VALUES (:eventId, :studentId, 'REGISTERED')
                """)
                .param("eventId", eventId)
                .param("studentId", studentId)
                .update();

        // 2. Đăng ký lần 2 cùng status REGISTERED -> bị unique index uq_registrations_active chặn
        assertThatThrownBy(() -> jdbcClient.sql("""
                INSERT INTO registrations (event_id, student_id, status)
                VALUES (:eventId, :studentId, 'REGISTERED')
                """)
                .param("eventId", eventId)
                .param("studentId", studentId)
                .update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_registrations_active");
    }

    @Test
    @DisplayName("Cho phép đăng ký mới khi bản ghi cũ đã CANCELLED")
    void registrationsAllowsReRegistrationAfterCancelled() {
        UUID ownerId = createTestUser("owner_reg_cancel");
        UUID studentId = createTestUser("student_reg_cancel");
        UUID eventId = createTestEvent(ownerId);

        // 1. Đăng ký và sau đó CANCELLED
        jdbcClient.sql("""
                INSERT INTO registrations (event_id, student_id, status)
                VALUES (:eventId, :studentId, 'CANCELLED')
                """)
                .param("eventId", eventId)
                .param("studentId", studentId)
                .update();

        // 2. Đăng ký lại REGISTERED -> Thành công
        int inserted = jdbcClient.sql("""
                INSERT INTO registrations (event_id, student_id, status)
                VALUES (:eventId, :studentId, 'REGISTERED')
                """)
                .param("eventId", eventId)
                .param("studentId", studentId)
                .update();

        assertThat(inserted).isEqualTo(1);
    }

    @Test
    @DisplayName("Chặn 2 lượt điểm danh ACCEPTED trùng lặp cho cùng 1 sinh viên trong sự kiện")
    void attendancesUniqueAcceptedConstraint() {
        UUID ownerId = createTestUser("owner_att_test");
        UUID studentId = createTestUser("student_att_test");
        UUID eventId = createTestEvent(ownerId);

        // 1. Điểm danh lần đầu ACCEPTED
        jdbcClient.sql("""
                INSERT INTO attendances (event_id, student_id, method, status, recorded_at)
                VALUES (:eventId, :studentId, 'DYNAMIC_QR', 'ACCEPTED', now())
                """)
                .param("eventId", eventId)
                .param("studentId", studentId)
                .update();

        // 2. Điểm danh lần 2 ACCEPTED -> bị unique index uq_attendances_valid chặn
        assertThatThrownBy(() -> jdbcClient.sql("""
                INSERT INTO attendances (event_id, student_id, method, status, recorded_at)
                VALUES (:eventId, :studentId, 'DYNAMIC_QR', 'ACCEPTED', now())
                """)
                .param("eventId", eventId)
                .param("studentId", studentId)
                .update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_attendances_valid");
    }

    @Test
    @DisplayName("Trigger audit_logs_immutable chặn lệnh UPDATE trên audit_logs")
    void auditLogsAreImmutableRejectsUpdate() {
        UUID auditId = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO audit_logs (id, action, target_type, outcome)
                VALUES (:id, 'TEST_ACTION', 'EVENT', 'SUCCESS')
                """)
                .param("id", auditId)
                .update();

        assertThatThrownBy(() -> jdbcClient.sql("""
                UPDATE audit_logs SET outcome = 'REJECTED' WHERE id = :id
                """)
                .param("id", auditId)
                .update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("audit_logs is append-only");
    }

    @Test
    @DisplayName("Trigger audit_logs_immutable chặn lệnh DELETE trên audit_logs")
    void auditLogsAreImmutableRejectsDelete() {
        UUID auditId = UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO audit_logs (id, action, target_type, outcome)
                VALUES (:id, 'TEST_ACTION', 'EVENT', 'SUCCESS')
                """)
                .param("id", auditId)
                .update();

        assertThatThrownBy(() -> jdbcClient.sql("""
                DELETE FROM audit_logs WHERE id = :id
                """)
                .param("id", auditId)
                .update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("audit_logs is append-only");
    }

    private UUID createTestUser(String loginId) {
        UUID id = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO app_user (id, login_id, password_hash, full_name)
                VALUES (:id, :loginId, 'hash', 'Test User')
                """)
                .param("id", id)
                .param("loginId", loginId + "_" + UUID.randomUUID().toString().substring(0, 8))
                .update();
        return id;
    }

    private UUID createTestEvent(UUID ownerId) {
        UUID id = UUID.randomUUID();
        jdbcClient.sql("""
                INSERT INTO events (
                    id, owner_id, title, location,
                    start_at, end_at, registration_deadline,
                    attendance_open_at, attendance_close_at,
                    quota_total, activity_type
                ) VALUES (
                    :id, :ownerId, 'Test Event', 'Hall A',
                    now() + interval '1 day', now() + interval '2 days', now() + interval '1 day',
                    now() + interval '1 day', now() + interval '2 days',
                    100, 'WORKSHOP'
                )
                """)
                .param("id", id)
                .param("ownerId", ownerId)
                .update();
        return id;
    }
}

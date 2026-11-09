package vn.edu.qnu.events.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class AuditIT {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private vn.edu.qnu.events.auth.AppUserRepository appUserRepository;

    @Test
    @DisplayName("1. Ghi log kiểm toán thành công qua AuditService và lưu đúng các trường bắt buộc")
    void recordAuditLogSuccess() {
        UUID actorId = appUserRepository.findByLoginId("sv001").orElseThrow().getId();
        String action = "TEST_AUDIT_ACTION";
        String targetType = "TEST_TARGET";
        String targetId = UUID.randomUUID().toString();

        AuditLog log = auditService.record(
                actorId,
                "STUDENT",
                action,
                targetType,
                targetId,
                "SUCCESS",
                null,
                Map.of("note", "test audit entry"),
                "req-12345"
        );

        assertThat(log.getId()).isNotNull();
        assertThat(log.getAction()).isEqualTo(action);
        assertThat(log.getOutcome()).isEqualTo("SUCCESS");
        assertThat(log.getDetails()).containsEntry("note", "test audit entry");
        assertThat(log.getRequestId()).isEqualTo("req-12345");
    }

    @Test
    @DisplayName("2. Trigger trg_audit_logs_immutable chặn UPDATE trên bảng audit_logs")
    void databaseRejectsUpdateOnAuditLogs() {
        AuditLog log = auditService.record(
                null,
                "SYSTEM",
                "SECURITY_TEST_UPDATE",
                "SYSTEM",
                "system-1",
                "SUCCESS",
                null,
                Map.of()
        );

        assertThatThrownBy(() -> {
            jdbcTemplate.update("UPDATE audit_logs SET action = 'MODIFIED_ACTION' WHERE id = ?", log.getId());
        }).hasMessageContaining("audit_logs is append-only");
    }

    @Test
    @DisplayName("3. Trigger trg_audit_logs_immutable chặn DELETE trên bảng audit_logs")
    void databaseRejectsDeleteOnAuditLogs() {
        AuditLog log = auditService.record(
                null,
                "SYSTEM",
                "SECURITY_TEST_DELETE",
                "SYSTEM",
                "system-2",
                "SUCCESS",
                null,
                Map.of()
        );

        assertThatThrownBy(() -> {
            jdbcTemplate.update("DELETE FROM audit_logs WHERE id = ?", log.getId());
        }).hasMessageContaining("audit_logs is append-only");
    }
}


package SeleniumTest.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight PostgreSQL JDBC helper for Selenium test fixture creation & teardown.
 */
public final class SeleniumDbHelper {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);
    private static volatile Boolean cachedAvailability;

    public boolean isAvailable() {
        if (!SeleniumConfig.isDbEnabled()) {
            return false;
        }
        if (cachedAvailability != null) {
            return cachedAvailability;
        }
        try {
            DriverManager.setLoginTimeout(3);
            try (Connection ignored = openConnection()) {
                cachedAvailability = true;
            }
        } catch (SQLException ex) {
            cachedAvailability = false;
        }
        return cachedAvailability;
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                SeleniumConfig.dbUrl(),
                SeleniumConfig.dbUsername(),
                SeleniumConfig.dbPassword()
        );
    }

    public UUID createTemporaryUser(
            String loginId,
            String rawPassword,
            String fullName,
            String studentCode,
            UUID classId,
            String role,
            String status
    ) throws SQLException {
        UUID userId = UUID.randomUUID();
        String hash = BCRYPT.encode(rawPassword);
        try (Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO app_user (id, login_id, password_hash, full_name, student_code, class_id, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    ps.setObject(1, userId);
                    ps.setString(2, loginId);
                    ps.setString(3, hash);
                    ps.setString(4, fullName);
                    if (studentCode != null) {
                        ps.setString(5, studentCode);
                    } else {
                        ps.setNull(5, Types.VARCHAR);
                    }
                    if (classId != null) {
                        ps.setObject(6, classId);
                    } else {
                        ps.setNull(6, Types.OTHER);
                    }
                    ps.setString(7, status != null ? status : "ACTIVE");
                    ps.executeUpdate();
                }

                if (role != null && !role.isBlank()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO user_role (user_id, role) VALUES (?, ?)")) {
                        ps.setObject(1, userId);
                        ps.setString(2, role);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
        return userId;
    }

    public void updateEventRegistrationDeadline(UUID eventId, Instant deadline) throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE events SET registration_deadline = ? WHERE id = ?")) {
            ps.setTimestamp(1, Timestamp.from(deadline));
            ps.setObject(2, eventId);
            ps.executeUpdate();
        }
    }

    public void deleteTemporaryUser(UUID userId) {
        if (userId == null || !isAvailable()) {
            return;
        }
        try (Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try {
                execUpdate(conn, "DELETE FROM attendance_attempts WHERE student_id = ?", userId);
                execUpdate(conn, "DELETE FROM attendances WHERE student_id = ?", userId);
                execUpdate(conn, "DELETE FROM registrations WHERE student_id = ?", userId);
                execUpdate(conn, "DELETE FROM auth_session WHERE user_id = ?", userId);
                execUpdate(conn, "DELETE FROM user_role WHERE user_id = ?", userId);
                execUpdate(conn, "DELETE FROM app_user WHERE id = ?", userId);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
            }
        } catch (SQLException ignored) {
        }
    }

    public void cleanupEvent(UUID eventId) {
        if (eventId == null || !isAvailable()) {
            return;
        }
        try (Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try {
                execUpdate(conn, "DELETE FROM attendance_attempts WHERE event_id = ?", eventId);
                execUpdate(conn, "DELETE FROM attendances WHERE event_id = ?", eventId);
                execUpdate(conn, "DELETE FROM registrations WHERE event_id = ?", eventId);
                execUpdate(conn, "DELETE FROM event_audience WHERE event_id = ?", eventId);
                execUpdate(conn, "DELETE FROM events WHERE id = ?", eventId);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
            }
        } catch (SQLException ignored) {
        }
    }

    private void execUpdate(Connection conn, String sql, Object param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            ps.executeUpdate();
        }
    }
}

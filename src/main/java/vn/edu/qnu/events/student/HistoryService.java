package vn.edu.qnu.events.student;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Service
public class HistoryService {

    private final JdbcTemplate jdbcTemplate;

    public HistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<HistoryItem> getStudentHistory(UUID studentId) {
        String sql = """
            SELECT 
                e.id AS event_id,
                e.title AS title,
                e.start_at AS start_at,
                e.location AS location,
                r.status AS registration_status,
                r.created_at AS registered_at,
                COALESCE(a.status, 'NOT_CHECKED_IN') AS attendance_status,
                a.recorded_at AS attendance_at,
                COALESCE(a.method, 'DYNAMIC_QR') AS method,
                e.activity_type AS activity_type,
                e.activity_points AS activity_points
            FROM registrations r
            JOIN events e ON r.event_id = e.id
            LEFT JOIN attendances a ON a.event_id = e.id AND a.student_id = r.student_id AND a.status = 'ACCEPTED'
            WHERE r.student_id = ?
            ORDER BY e.start_at DESC, r.created_at DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp startAtTs = rs.getTimestamp("start_at");
            Timestamp registeredAtTs = rs.getTimestamp("registered_at");
            Timestamp attendanceAtTs = rs.getTimestamp("attendance_at");

            return new HistoryItem(
                    rs.getObject("event_id", UUID.class),
                    rs.getString("title"),
                    startAtTs != null ? startAtTs.toInstant() : null,
                    rs.getString("location"),
                    rs.getString("registration_status"),
                    registeredAtTs != null ? registeredAtTs.toInstant() : null,
                    rs.getString("attendance_status"),
                    attendanceAtTs != null ? attendanceAtTs.toInstant() : null,
                    rs.getString("method"),
                    rs.getString("activity_type"),
                    rs.getInt("activity_points")
            );
        }, studentId);
    }
}


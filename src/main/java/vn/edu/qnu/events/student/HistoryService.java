package vn.edu.qnu.events.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class HistoryService {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public HistoryService(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<HistoryItem> getStudentHistory(UUID studentId) {
        return getStudentHistory(studentId, HistoryFilter.empty(), Pageable.unpaged()).getContent();
    }

    @Transactional(readOnly = true)
    public Page<HistoryItem> getStudentHistory(UUID studentId, HistoryFilter filter, Pageable pageable) {
        if (filter == null) {
            filter = HistoryFilter.empty();
        }
        if (pageable == null) {
            pageable = Pageable.unpaged();
        }

        // Mỗi sự kiện chỉ lấy registration MỚI NHẤT của sinh viên (hủy rồi đăng ký lại tạo nhiều dòng),
        // khử trùng trước khi lọc/đếm/phân trang để totalElements và LIMIT/OFFSET tính theo số sự kiện.
        StringBuilder whereSql = new StringBuilder("""
            FROM (
                SELECT DISTINCT ON (reg.event_id)
                    reg.id, reg.event_id, reg.student_id, reg.status, reg.created_at
                FROM registrations reg
                WHERE reg.student_id = ?
                ORDER BY reg.event_id, reg.created_at DESC, reg.id DESC
            ) r
            JOIN events e ON r.event_id = e.id
            LEFT JOIN attendances a ON a.event_id = e.id AND a.student_id = r.student_id AND a.status = 'ACCEPTED'
            WHERE 1 = 1
        """);

        List<Object> params = new ArrayList<>();
        params.add(studentId);

        // 1. Lọc theo năm học và học kỳ
        DateRange dateRange = resolveDateRange(filter.academicYear(), filter.semester());
        if (dateRange != null) {
            whereSql.append(" AND e.start_at >= ? AND e.start_at <= ?");
            params.add(Timestamp.from(dateRange.from()));
            params.add(Timestamp.from(dateRange.to()));
        }

        // 2. Lọc theo trạng thái điểm danh
        if (filter.attendanceStatus() != null && !filter.attendanceStatus().isBlank()) {
            String status = filter.attendanceStatus().trim().toUpperCase();
            if ("ACCEPTED".equals(status)) {
                whereSql.append(" AND a.status = 'ACCEPTED'");
            } else if ("NOT_CHECKED_IN".equals(status)) {
                whereSql.append(" AND (a.id IS NULL OR a.status != 'ACCEPTED')");
            }
        }

        // 3. Lọc sự kiện sắp diễn ra / đã qua
        if (filter.upcoming() != null) {
            Instant now = Instant.now(clock);
            if (filter.upcoming()) {
                whereSql.append(" AND e.start_at > ?");
            } else {
                whereSql.append(" AND e.start_at <= ?");
            }
            params.add(Timestamp.from(now));
        }

        // 4. Lọc theo loại hoạt động
        if (filter.activityType() != null && !filter.activityType().isBlank()) {
            whereSql.append(" AND LOWER(e.activity_type) = LOWER(?)");
            params.add(filter.activityType().trim());
        }

        // Đếm tổng số bản ghi (cho phân trang)
        String countSql = "SELECT COUNT(*) " + whereSql;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long totalElements = total != null ? total : 0L;

        // Truy vấn dữ liệu với thứ tự sắp xếp và giới hạn phân trang
        StringBuilder dataSql = new StringBuilder("""
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
        """);
        dataSql.append(whereSql);
        dataSql.append(buildOrderBy(pageable.getSort()));

        List<Object> dataParams = new ArrayList<>(params);
        if (pageable.isPaged()) {
            dataSql.append(" LIMIT ? OFFSET ?");
            dataParams.add(pageable.getPageSize());
            dataParams.add(pageable.getOffset());
        }

        List<HistoryItem> items = jdbcTemplate.query(dataSql.toString(), (rs, rowNum) -> {
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
        }, dataParams.toArray());

        return new PageImpl<>(items, pageable, totalElements);
    }

    private DateRange resolveDateRange(String academicYear, Integer semester) {
        if (academicYear == null || academicYear.isBlank()) {
            return null;
        }
        String trimmed = academicYear.trim();
        int startYear;
        int endYear;
        if (trimmed.contains("-")) {
            String[] parts = trimmed.split("-");
            try {
                startYear = Integer.parseInt(parts[0].trim());
                endYear = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            try {
                startYear = Integer.parseInt(trimmed);
                endYear = startYear + 1;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        ZoneOffset offset = ZoneOffset.UTC;
        if (semester != null) {
            if (semester == 1) {
                // Học kỳ 1: từ 01/09/startYear đến 31/01/endYear
                Instant from = LocalDate.of(startYear, 9, 1).atStartOfDay().toInstant(offset);
                Instant to = LocalDate.of(endYear, 1, 31).atTime(23, 59, 59, 999_999_999).toInstant(offset);
                return new DateRange(from, to);
            } else if (semester == 2) {
                // Học kỳ 2: từ 01/02/endYear đến 30/06/endYear
                Instant from = LocalDate.of(endYear, 2, 1).atStartOfDay().toInstant(offset);
                Instant to = LocalDate.of(endYear, 6, 30).atTime(23, 59, 59, 999_999_999).toInstant(offset);
                return new DateRange(from, to);
            } else if (semester == 3) {
                // Học kỳ 3 (Hè): từ 01/07/endYear đến 31/08/endYear
                Instant from = LocalDate.of(endYear, 7, 1).atStartOfDay().toInstant(offset);
                Instant to = LocalDate.of(endYear, 8, 31).atTime(23, 59, 59, 999_999_999).toInstant(offset);
                return new DateRange(from, to);
            }
        }
        // Toàn bộ năm học: từ 01/09/startYear đến 31/08/endYear
        Instant from = LocalDate.of(startYear, 9, 1).atStartOfDay().toInstant(offset);
        Instant to = LocalDate.of(endYear, 8, 31).atTime(23, 59, 59, 999_999_999).toInstant(offset);
        return new DateRange(from, to);
    }

    private String buildOrderBy(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return " ORDER BY e.start_at DESC, r.created_at DESC, e.id ASC";
        }
        List<String> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            String property = order.getProperty();
            String column = switch (property) {
                case "startAt" -> "e.start_at";
                case "title" -> "e.title";
                case "registeredAt" -> "r.created_at";
                case "attendanceAt" -> "a.recorded_at";
                case "activityPoints" -> "e.activity_points";
                default -> null;
            };
            if (column != null) {
                orders.add(column + " " + (order.isAscending() ? "ASC" : "DESC"));
            }
        }
        if (orders.isEmpty()) {
            return " ORDER BY e.start_at DESC, r.created_at DESC, e.id ASC";
        }
        // Tie-breaker e.id để thứ tự ổn định giữa các trang khi giá trị sort trùng nhau
        return " ORDER BY " + String.join(", ", orders) + ", e.id ASC";
    }

    private record DateRange(Instant from, Instant to) {
    }
}

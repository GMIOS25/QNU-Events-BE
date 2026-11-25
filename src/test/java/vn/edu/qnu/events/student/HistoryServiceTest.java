package vn.edu.qnu.events.student;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HistoryServiceTest {

    private final HistoryService historyService = new HistoryService(
            mock(JdbcTemplate.class),
            Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    @DisplayName("academicYear thiếu phần sau dấu '-' hoặc quá nhiều dấu '-' không ném 500 (BUG-06)")
    void malformedAcademicYearReturnsNullSafely() {
        assertThat(historyService.resolveDateRange("2026-", 1)).isNull();
        assertThat(historyService.resolveDateRange("-2027", 1)).isNull();
        assertThat(historyService.resolveDateRange("2026-2027-2028", 1)).isNull();
        assertThat(historyService.resolveDateRange("9999999999", 1)).isNull();
        assertThat(historyService.resolveDateRange("2027-2026", 1)).isNull();
        assertThat(historyService.resolveDateRange("abc", null)).isNull();
    }

    @Test
    @DisplayName("academicYear và semester hợp lệ trả về khoảng thời gian chính xác")
    void validAcademicYearResolvesCorrectRange() {
        var sem1 = historyService.resolveDateRange("2026-2027", 1);
        assertThat(sem1).isNotNull();
        assertThat(sem1.from()).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
        assertThat(sem1.to()).isEqualTo(Instant.parse("2027-01-31T23:59:59.999999999Z"));

        var fullYear = historyService.resolveDateRange("2026", null);
        assertThat(fullYear).isNotNull();
        assertThat(fullYear.from()).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
        assertThat(fullYear.to()).isEqualTo(Instant.parse("2027-08-31T23:59:59.999999999Z"));
    }
}

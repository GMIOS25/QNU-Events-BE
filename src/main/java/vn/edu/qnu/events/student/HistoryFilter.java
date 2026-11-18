package vn.edu.qnu.events.student;

public record HistoryFilter(
        String academicYear,
        Integer semester,
        String attendanceStatus,
        Boolean upcoming,
        String activityType
) {
    public static HistoryFilter empty() {
        return new HistoryFilter(null, null, null, null, null);
    }
}

package vn.edu.qnu.events.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 400 Bad Request
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ."),

    // 401 Unauthorized
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập để thực hiện."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không chính xác."),
    SESSION_REVOKED(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã bị thu hồi hoặc hết hạn."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này."),
    NOT_IN_AUDIENCE(HttpStatus.FORBIDDEN, "Sự kiện không dành cho lớp của bạn."),
    NOT_REGISTERED(HttpStatus.FORBIDDEN, "Bạn chưa có suất đăng ký sự kiện này."),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu."),

    // 409 Conflict
    EVENT_NOT_PUBLISHED(HttpStatus.CONFLICT, "Sự kiện chưa được công bố."),
    REGISTRATION_CLOSED(HttpStatus.CONFLICT, "Thời gian đăng ký sự kiện đã kết thúc."),
    CAPACITY_FULL(HttpStatus.CONFLICT, "Sự kiện đã hết suất tham gia."),
    PREVIEW_REQUIRED(HttpStatus.CONFLICT, "Cần xem trước sự kiện trước khi công bố."),
    PREVIEW_OUTDATED(HttpStatus.CONFLICT, "Nội dung sự kiện đã thay đổi, vui lòng xem trước lại."),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu đã bị thay đổi bởi người khác, vui lòng tải lại."),
    ATTENDANCE_NOT_OPEN(HttpStatus.CONFLICT, "Chưa đến hoặc đã hết thời gian điểm danh."),
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "Bạn đã điểm danh sự kiện này rồi."),
    ATTEMPT_LIMIT_REACHED(HttpStatus.CONFLICT, "Bạn đã vượt quá số lần thử điểm danh cho phép."),

    // 422 Unprocessable Entity
    QR_MALFORMED(HttpStatus.UNPROCESSABLE_ENTITY, "Mã QR không đúng định dạng."),
    QR_BAD_SIGNATURE(HttpStatus.UNPROCESSABLE_ENTITY, "Chữ ký mã QR không hợp lệ."),
    QR_WRONG_EVENT(HttpStatus.UNPROCESSABLE_ENTITY, "Mã QR không thuộc về sự kiện này."),
    QR_EXPIRED(HttpStatus.UNPROCESSABLE_ENTITY, "Mã QR đã hết hạn."),
    QR_FROM_FUTURE(HttpStatus.UNPROCESSABLE_ENTITY, "Mã QR không hợp lệ (thời gian tương lai)."),

    // 500 Internal Server Error
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}


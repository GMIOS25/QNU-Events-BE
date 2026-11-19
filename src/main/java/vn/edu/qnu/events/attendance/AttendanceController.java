package vn.edu.qnu.events.attendance;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static vn.edu.qnu.events.common.GlobalExceptionHandler.resolveTraceId;

@RestController
@RequestMapping("/api/v1/events/{id}")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/qr")
    public ResponseEntity<EventQrResponse> getQrToken(@PathVariable("id") UUID id) {
        EventQrResponse response = attendanceService.getQrToken(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/attendance/qr")
    public ResponseEntity<String> checkInWithQr(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CheckInRequest request,
            HttpServletRequest httpRequest
    ) {
        CheckInResult result = attendanceService.checkInWithQr(
                id, idempotencyKey, request, resolveTraceId(httpRequest)
        );
        return ResponseEntity.status(result.httpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(result.responseBody());
    }
}


package vn.edu.qnu.events.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceAttemptRepository extends JpaRepository<AttendanceAttempt, UUID> {

    Optional<AttendanceAttempt> findByEventIdAndStudentIdAndIdempotencyKey(UUID eventId, UUID studentId, String idempotencyKey);

    long countByEventIdAndStudentIdAndCountedTrue(UUID eventId, UUID studentId);
}


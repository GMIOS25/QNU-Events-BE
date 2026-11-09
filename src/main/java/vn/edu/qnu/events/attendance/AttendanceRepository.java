package vn.edu.qnu.events.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    boolean existsByEventIdAndStudentIdAndStatus(UUID eventId, UUID studentId, String status);

    Optional<Attendance> findByEventIdAndStudentIdAndStatus(UUID eventId, UUID studentId, String status);

    long countByEventIdAndStatus(UUID eventId, String status);
}


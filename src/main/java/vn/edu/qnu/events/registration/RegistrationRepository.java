package vn.edu.qnu.events.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    @Query("""
        SELECT r FROM Registration r
        WHERE r.eventId = :eventId AND r.studentId = :studentId
        AND r.status IN ('REGISTERED', 'WAITING_LIST')
    """)
    Optional<Registration> findActiveByEventAndStudent(
            @Param("eventId") UUID eventId,
            @Param("studentId") UUID studentId
    );

    @Query("""
        SELECT r FROM Registration r
        WHERE r.eventId = :eventId AND r.studentId = :studentId
        ORDER BY r.createdAt DESC
        LIMIT 1
    """)
    Optional<Registration> findLatestByEventAndStudent(
            @Param("eventId") UUID eventId,
            @Param("studentId") UUID studentId
    );

    @Modifying
    @Query(value = """
        INSERT INTO registrations (id, event_id, student_id, status, created_at, updated_at)
        VALUES (:id, :eventId, :studentId, 'REGISTERED', :now, :now)
        ON CONFLICT (event_id, student_id) WHERE status IN ('REGISTERED', 'WAITING_LIST')
        DO NOTHING
    """, nativeQuery = true)
    int tryInsertRegistration(
            @Param("id") UUID id,
            @Param("eventId") UUID eventId,
            @Param("studentId") UUID studentId,
            @Param("now") Instant now
    );

    @Modifying
    @Query(value = """
        UPDATE events
        SET registered_count = registered_count + 1
        WHERE id = :eventId AND registered_count < quota_total
    """, nativeQuery = true)
    int reserveSeat(@Param("eventId") UUID eventId);

    long countByEventId(UUID eventId);
}


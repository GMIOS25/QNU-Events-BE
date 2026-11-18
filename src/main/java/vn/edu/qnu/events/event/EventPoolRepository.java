package vn.edu.qnu.events.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventPoolRepository extends JpaRepository<EventPool, UUID> {

    List<EventPool> findByEventIdOrderByPriorityAsc(UUID eventId);

    @Modifying
    @Query("UPDATE EventPool p SET p.used = p.used + 1 WHERE p.id = :id AND p.used < p.capacity")
    int claimSeat(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE EventPool p SET p.used = p.used - 1 WHERE p.id = :id AND p.used > 0")
    int releaseSeat(@Param("id") UUID id);
}

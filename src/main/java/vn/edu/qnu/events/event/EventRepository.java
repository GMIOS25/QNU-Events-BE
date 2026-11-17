package vn.edu.qnu.events.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Event e SET e.previewedVersion = e.version WHERE e.id = :id")
    int updatePreviewedVersion(@Param("id") UUID id);

    @Query("SELECT e FROM Event e WHERE e.ownerId = :ownerId AND (:q IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Event> findByOwnerId(@Param("ownerId") UUID ownerId, @Param("q") String q, Pageable pageable);

    @Query("""
        SELECT DISTINCT e FROM Event e
        JOIN e.audienceClassIds c
        WHERE e.status = 'PUBLISHED'
        AND c = :classId
        AND (:q IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    Page<Event> findPublishedForClass(@Param("classId") UUID classId, @Param("q") String q, Pageable pageable);

    @Query("""
        SELECT e FROM Event e
        JOIN e.audienceClassIds c
        WHERE e.id = :id
        AND e.status = 'PUBLISHED'
        AND c = :classId
    """)
    Optional<Event> findPublishedByIdAndClass(@Param("id") UUID id, @Param("classId") UUID classId);
}


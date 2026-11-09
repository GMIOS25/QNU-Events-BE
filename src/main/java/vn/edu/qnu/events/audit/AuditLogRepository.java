package vn.edu.qnu.events.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTargetTypeAndTargetIdOrderByOccurredAtAsc(String targetType, String targetId);
    List<AuditLog> findByActionOrderByOccurredAtDesc(String action);
}


package vn.edu.qnu.events.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditService(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(
            UUID actorId,
            String actorRole,
            String action,
            String targetType,
            String targetId,
            String outcome,
            String reasonCode,
            Map<String, Object> details,
            String requestId
    ) {
        AuditLog auditLog = new AuditLog(
                Instant.now(clock),
                actorId,
                actorRole,
                action,
                targetType,
                targetId,
                outcome,
                reasonCode,
                details != null ? details : Collections.emptyMap(),
                requestId
        );

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit logged: action={}, targetType={}, targetId={}, outcome={}, reason={}",
                action, targetType, targetId, outcome, reasonCode);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(
            UUID actorId,
            String actorRole,
            String action,
            String targetType,
            String targetId,
            String outcome,
            String reasonCode,
            Map<String, Object> details
    ) {
        return record(actorId, actorRole, action, targetType, targetId, outcome, reasonCode, details, null);
    }
}


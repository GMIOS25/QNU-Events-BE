package vn.edu.qnu.events.event;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_pools")
public class EventPool {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private PoolScopeType scopeType;

    @Column(name = "scope_ref", nullable = false, length = 64)
    private String scopeRef = "";

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int used = 0;

    @Column(nullable = false)
    private int priority = 0;

    @Column(name = "allow_overflow", nullable = false)
    private boolean allowOverflow = false;

    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory = false;

    @Column(name = "reserved_until")
    private Instant reservedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected EventPool() { }

    public EventPool(UUID eventId, PoolScopeType scopeType, String scopeRef,
                     int capacity, int priority, boolean allowOverflow, boolean mandatory) {
        this.eventId = eventId;
        this.scopeType = scopeType;
        this.scopeRef = scopeRef == null ? "" : scopeRef;
        this.capacity = capacity;
        this.priority = priority;
        this.allowOverflow = allowOverflow;
        this.mandatory = mandatory;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public PoolScopeType getScopeType() { return scopeType; }
    public String getScopeRef() { return scopeRef; }
    public int getCapacity() { return capacity; }
    public int getUsed() { return used; }
    public int getPriority() { return priority; }
    public boolean isAllowOverflow() { return allowOverflow; }
    public boolean isMandatory() { return mandatory; }
    public Instant getReservedUntil() { return reservedUntil; }
}

package vn.edu.qnu.events.common;

import java.util.List;
import java.util.UUID;

public record CurrentUser(
        UUID userId,
        UUID sessionId,
        List<String> roles,
        UUID classId
) {
    public boolean hasRole(String role) {
        if (roles == null) return false;
        String normalized = role.startsWith("ROLE_") ? role.substring(5) : role;
        return roles.stream().anyMatch(r -> {
            String existing = r.startsWith("ROLE_") ? r.substring(5) : r;
            return existing.equalsIgnoreCase(normalized);
        });
    }

    public boolean isStudent() {
        return hasRole("STUDENT");
    }

    public boolean isEventOwner() {
        return hasRole("EVENT_OWNER");
    }

    public boolean isClassRepresentative() {
        return hasRole("CLASS_REPRESENTATIVE");
    }

    public boolean isSystemAdmin() {
        return hasRole("SYSTEM_ADMIN");
    }
}


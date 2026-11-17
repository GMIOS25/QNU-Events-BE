package vn.edu.qnu.events.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class CurrentUserProvider {

    private CurrentUserProvider() {
    }

    public static CurrentUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }

        if (auth.getPrincipal() instanceof Jwt jwt) {
            UUID userId = UUID.fromString(jwt.getSubject());

            String sidClaim = jwt.getClaimAsString("sid");
            UUID sessionId = sidClaim != null ? UUID.fromString(sidClaim) : null;

            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                roles = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                        .toList();
            }

            String classIdClaim = jwt.getClaimAsString("classId");
            UUID classId = (classIdClaim != null && !classIdClaim.isBlank()) ? UUID.fromString(classIdClaim) : null;

            return new CurrentUser(userId, sessionId, roles, classId);
        }

        // Fallback for custom authentication in test mocks
        try {
            UUID userId = UUID.fromString(auth.getName());
            List<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                    .toList();
            return new CurrentUser(userId, null, roles, null);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Principal không hợp lệ: " + auth.getName());
        }
    }
}


package vn.edu.qnu.events.auth;

import java.util.List;
import java.util.UUID;

public record UserInfo(
        UUID id,
        String loginId,
        String fullName,
        String studentCode,
        List<String> roles,
        UUID classId
) {
    public static UserInfo from(AppUser user) {
        return new UserInfo(
                user.getId(),
                user.getLoginId(),
                user.getFullName(),
                user.getStudentCode(),
                user.getRoles().stream().sorted().toList(),
                user.getClassId()
        );
    }
}


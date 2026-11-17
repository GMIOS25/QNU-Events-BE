package vn.edu.qnu.events.student;

import java.util.UUID;

public record ClassResponse(
        UUID id,
        String code,
        String name,
        String facultyName
) {
    public static ClassResponse from(OrgClass orgClass) {
        return new ClassResponse(
                orgClass.getId(),
                orgClass.getCode(),
                orgClass.getName(),
                orgClass.getFacultyName()
        );
    }
}


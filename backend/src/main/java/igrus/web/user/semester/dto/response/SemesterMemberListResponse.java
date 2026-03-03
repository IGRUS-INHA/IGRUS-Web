package igrus.web.user.semester.dto.response;

import igrus.web.user.domain.UserRole;

public record SemesterMemberListResponse(
        Long userId,
        String studentId,
        String name,
        String department,
        String email,
        String phoneNumber,
        UserRole role,
        boolean isWithdrawn,
        Integer grade,
        String motivation
) {}

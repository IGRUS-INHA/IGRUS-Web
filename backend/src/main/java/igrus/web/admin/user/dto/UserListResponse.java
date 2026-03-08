package igrus.web.admin.user.dto;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;

import java.time.Instant;

public record UserListResponse(
        Long userId,

        String studentId,

        String name,

        String email,

        UserRole role,

        UserStatus status,

        String department,

        String phoneNumber,

        Instant createdAt
) {
    public static UserListResponse from(User user) {
        return new UserListResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getDepartment(),
                user.getPhoneNumber(),
                user.getCreatedAt()
        );
    }
}

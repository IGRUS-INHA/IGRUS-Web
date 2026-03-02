package igrus.web.admin.user.dto;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;

import java.time.Instant;

public record UserRoleHistoryResponse(
        Long id,

        Long userId,

        String userName,

        String studentId,

        UserRole previousRole,

        UserRole newRole,

        String reason,

        Long changedBy,

        Instant changedAt
) {
    public static UserRoleHistoryResponse from(UserRoleHistory history) {
        User user = history.getUser();
        return new UserRoleHistoryResponse(
                history.getId(),
                history.getUserId(),
                user != null ? user.getName() : User.WITHDRAWN_DISPLAY_NAME,
                user != null ? user.getStudentId() : null,
                history.getPreviousRole(),
                history.getNewRole(),
                history.getReason(),
                history.getCreatedBy(),
                history.getCreatedAt()
        );
    }
}

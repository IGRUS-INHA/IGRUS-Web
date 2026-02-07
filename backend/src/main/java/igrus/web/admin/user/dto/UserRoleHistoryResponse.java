package igrus.web.admin.user.dto;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "권한 변경 이력 응답")
public record UserRoleHistoryResponse(
        @Schema(description = "이력 ID", example = "1")
        Long id,

        @Schema(description = "대상 사용자 ID", example = "10")
        Long userId,

        @Schema(description = "대상 사용자 이름", example = "홍길동")
        String userName,

        @Schema(description = "대상 사용자 학번", example = "20231234")
        String studentId,

        @Schema(description = "변경 전 역할", example = "ASSOCIATE")
        UserRole previousRole,

        @Schema(description = "변경 후 역할", example = "MEMBER")
        UserRole newRole,

        @Schema(description = "변경 사유")
        String reason,

        @Schema(description = "변경자 ID", example = "1")
        Long changedBy,

        @Schema(description = "변경 일시", example = "2025-01-15T10:30:00Z")
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

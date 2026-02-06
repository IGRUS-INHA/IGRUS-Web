package igrus.web.admin.user.dto;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "관리자용 회원 목록 응답")
public record UserListResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "학번", example = "20231234")
        String studentId,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "이메일", example = "hong@inha.edu")
        String email,

        @Schema(description = "역할", example = "MEMBER")
        UserRole role,

        @Schema(description = "상태", example = "ACTIVE")
        UserStatus status,

        @Schema(description = "가입일", example = "2025-01-15T10:30:00Z")
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
                user.getCreatedAt()
        );
    }
}

package igrus.web.user.mypage.dto.response;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 내 프로필 조회 응답 DTO.
 */
@Schema(description = "내 프로필 정보")
public record MyProfileResponse(
        @Schema(description = "학번", example = "12201234")
        String studentId,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "이메일", example = "hong@example.com")
        String email,

        @Schema(description = "학과", example = "컴퓨터공학과")
        String department,

        @Schema(description = "역할", example = "MEMBER")
        UserRole role,

        @Schema(description = "가입일")
        Instant createdAt,

        @Schema(description = "정회원 승인일 (준회원인 경우 null)")
        Instant approvedAt
) {
    public static MyProfileResponse from(User user, Instant approvedAt) {
        return new MyProfileResponse(
                user.getStudentId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getRole(),
                user.getCreatedAt(),
                approvedAt
        );
    }
}

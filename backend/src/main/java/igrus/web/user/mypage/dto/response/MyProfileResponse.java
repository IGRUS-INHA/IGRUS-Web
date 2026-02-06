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

        @Schema(description = "전화번호", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "학과", example = "컴퓨터공학과")
        String department,

        @Schema(description = "역할", example = "MEMBER")
        UserRole role,

        @Schema(description = "가입일")
        Instant createdAt
) {
    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getStudentId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getDepartment(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

package igrus.web.admin.user.dto;

import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "관리자용 회원 상세 응답")
public record UserDetailResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "학번", example = "20231234")
        String studentId,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "이메일", example = "hong@inha.edu")
        String email,

        @Schema(description = "전화번호", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "학과", example = "컴퓨터공학과")
        String department,

        @Schema(description = "가입 동기", example = "프로그래밍을 배우고 싶어서")
        String motivation,

        @Schema(description = "성별", example = "MALE")
        Gender gender,

        @Schema(description = "학년", example = "2")
        int grade,

        @Schema(description = "역할", example = "MEMBER")
        UserRole role,

        @Schema(description = "상태", example = "ACTIVE")
        UserStatus status,

        @Schema(description = "가입일", example = "2025-01-15T10:30:00Z")
        Instant createdAt
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getDepartment(),
                user.getMotivation(),
                user.getGender(),
                user.getGrade(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}

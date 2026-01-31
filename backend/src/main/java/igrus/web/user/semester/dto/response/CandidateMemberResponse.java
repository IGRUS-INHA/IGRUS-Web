package igrus.web.user.semester.dto.response;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학기별 회원 등록 후보 응답")
public record CandidateMemberResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,
        @Schema(description = "학번", example = "12345678")
        String studentId,
        @Schema(description = "이름", example = "홍길동")
        String name,
        @Schema(description = "학과", example = "컴퓨터공학과")
        String department,
        @Schema(description = "현재 역할", example = "MEMBER")
        UserRole role,
        @Schema(description = "해당 학기 등록 여부")
        boolean alreadyRegistered
) {
    public static CandidateMemberResponse from(User user, boolean alreadyRegistered) {
        return new CandidateMemberResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getDepartment(),
                user.getRole(),
                alreadyRegistered
        );
    }
}

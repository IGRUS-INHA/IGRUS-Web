package igrus.web.user.semester.dto.response;

import igrus.web.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학기별 회원 목록 응답")
public record SemesterMemberListResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,
        @Schema(description = "학번", example = "12345678")
        String studentId,
        @Schema(description = "이름", example = "홍길동")
        String name,
        @Schema(description = "학과", example = "컴퓨터공학과")
        String department,
        @Schema(description = "이메일", example = "user@example.com")
        String email,
        @Schema(description = "전화번호", example = "010-1234-5678")
        String phoneNumber,
        @Schema(description = "등록 시점 역할", example = "MEMBER")
        UserRole role,
        @Schema(description = "탈퇴 여부")
        boolean isWithdrawn
) {}

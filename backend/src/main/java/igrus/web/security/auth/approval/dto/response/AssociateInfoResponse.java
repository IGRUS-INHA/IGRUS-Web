package igrus.web.security.auth.approval.dto.response;

import igrus.web.user.domain.User;
import igrus.web.user.domain.Wish;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "준회원(가입 승인 대기자) 정보 응답")
public record AssociateInfoResponse(
        @Schema(description = "사용자 고유 ID", example = "1")
        Long userId,

        @Schema(description = "학번", example = "12345678")
        String studentId,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "학과", example = "컴퓨터공학과")
        String department,

        @Schema(description = "가입 동기", example = "웹 개발 역량을 키우고 싶습니다.")
        String motivation,

        @Schema(description = "가입 목적")
        List<Wish> wishes,

        @Schema(description = "가입 신청 일시", example = "2024-01-15T10:30:00Z")
        Instant createdAt,

        @Schema(description = "강등된 유저 여부", example = "false")
        boolean demoted
) {
    public static AssociateInfoResponse from(User user, boolean demoted) {
        return new AssociateInfoResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getDepartment(),
                user.getMotivation(),
                user.getWishes(),
                user.getCreatedAt(),
                demoted
        );
    }
}

package igrus.web.security.auth.approval.dto.response;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.user.domain.User;
import igrus.web.user.domain.Wish;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "거절된 준회원 정보 응답")
public record RejectedAssociateInfoResponse(
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

        @Schema(description = "거절 사유", example = "가입 동기가 불충분합니다.")
        String rejectionReason,

        @Schema(description = "거절 일시", example = "2024-01-20T14:00:00Z")
        Instant rejectedAt,

        @Schema(description = "거절 처리자 ID", example = "100")
        Long rejectedBy
) {
    public static RejectedAssociateInfoResponse from(AssociateDecision decision) {
        User user = decision.getUser();
        return new RejectedAssociateInfoResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getDepartment(),
                user.getMotivation(),
                user.getWishes(),
                user.getCreatedAt(),
                decision.getReason(),
                decision.getDecidedAt(),
                decision.getDecidedBy()
        );
    }
}

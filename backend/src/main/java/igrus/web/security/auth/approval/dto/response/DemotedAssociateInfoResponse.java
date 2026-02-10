package igrus.web.security.auth.approval.dto.response;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "강등된 준회원 정보 응답")
public record DemotedAssociateInfoResponse(
        @Schema(description = "사용자 고유 ID", example = "1")
        Long userId,

        @Schema(description = "학번", example = "12345678")
        String studentId,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "학과", example = "컴퓨터공학과")
        String department,

        @Schema(description = "강등 일시", example = "2024-01-20T14:00:00Z")
        Instant demotedAt,

        @Schema(description = "강등 처리자 ID", example = "100")
        Long demotedBy
) {
    public static DemotedAssociateInfoResponse from(AssociateDecision decision) {
        User user = decision.getUser();
        return new DemotedAssociateInfoResponse(
                user.getId(),
                user.getStudentId(),
                user.getName(),
                user.getDepartment(),
                decision.getDecidedAt(),
                decision.getDecidedBy()
        );
    }
}

package igrus.web.user.dto.response;

import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.AccountStatusChangeHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "계정 상태 변경 감사 이력 응답")
public record AccountStatusChangeHistoryResponse(
        @Schema(description = "이력 ID") Long id,
        @Schema(description = "대상 사용자 ID") Long userId,
        @Schema(description = "대상 사용자 학번") String userStudentId,
        @Schema(description = "변경자 사용자 ID") Long changedByUserId,
        @Schema(description = "변경자 학번") String changedByStudentId,
        @Schema(description = "변경 유형") AccountChangeType changeType,
        @Schema(description = "변경 전 값") String previousValue,
        @Schema(description = "변경 후 값") String newValue,
        @Schema(description = "변경 사유") String reason,
        @Schema(description = "변경 일시") Instant createdAt
) {
    public static AccountStatusChangeHistoryResponse from(AccountStatusChangeHistory history) {
        return new AccountStatusChangeHistoryResponse(
                history.getId(),
                history.getUserId(),
                history.getUserStudentId(),
                history.getChangedByUserId(),
                history.getChangedByStudentId(),
                history.getChangeType(),
                history.getPreviousValue(),
                history.getNewValue(),
                history.getReason(),
                history.getCreatedAt()
        );
    }
}

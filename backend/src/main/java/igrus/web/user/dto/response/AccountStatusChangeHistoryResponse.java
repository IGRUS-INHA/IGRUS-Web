package igrus.web.user.dto.response;

import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.AccountStatusChangeHistory;

import java.time.Instant;

public record AccountStatusChangeHistoryResponse(
        Long id,
        Long userId,
        String userStudentId,
        Long changedByUserId,
        String changedByStudentId,
        AccountChangeType changeType,
        String previousValue,
        String newValue,
        String reason,
        Instant createdAt
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

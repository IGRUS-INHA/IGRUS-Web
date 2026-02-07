package igrus.web.user.event;

import igrus.web.user.domain.AccountChangeType;

public record AccountStatusChangeEvent(
        Long userId,
        Long changedByUserId,
        AccountChangeType changeType,
        String previousValue,
        String newValue,
        String reason
) {
}

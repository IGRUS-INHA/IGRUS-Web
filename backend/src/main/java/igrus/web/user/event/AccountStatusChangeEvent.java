package igrus.web.user.event;

import igrus.web.user.domain.AccountChangeType;

import java.util.Objects;

public record AccountStatusChangeEvent(
        Long userId,
        Long changedByUserId,
        AccountChangeType changeType,
        String previousValue,
        String newValue,
        String reason
) {
    public AccountStatusChangeEvent {
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(previousValue, "previousValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
    }
}

package igrus.web.user.audit;

import igrus.web.user.domain.AccountChangeType;

import java.util.Objects;

public record AccountStatusChanged(
        Long userId,
        Long changedByUserId,
        AccountChangeType changeType,
        String previousValue,
        String newValue,
        String reason
) {
    public AccountStatusChanged {
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(previousValue, "previousValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
    }
}

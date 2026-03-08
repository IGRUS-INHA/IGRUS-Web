package igrus.web.event.audit;

import igrus.web.event.domain.EventChangeType;

import java.util.Objects;

/**
 * 행사 상태 변경 감사 이벤트.
 * {@link igrus.web.event.service.RecordEventStatusChangeService}에서 수신하여
 * 감사 이력을 기록합니다.
 */
public record EventStatusChanged(
        Long eventId,
        Long changedByUserId,
        EventChangeType changeType,
        String previousValue,
        String newValue,
        String reason
) {
    public EventStatusChanged {
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(previousValue, "previousValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
    }
}

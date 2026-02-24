package igrus.web.event.event;

import igrus.web.event.domain.EventChangeType;

import java.util.Objects;

/**
 * 행사 상태 변경 이벤트.
 * {@link igrus.web.event.service.RecordEventStatusChangeService}에서 수신하여
 * 감사 이력을 기록합니다.
 */
public record EventStatusChangeEvent(
        Long eventId,
        Long changedByUserId,
        EventChangeType changeType,
        String previousValue,
        String newValue,
        String reason
) {
    public EventStatusChangeEvent {
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(previousValue, "previousValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
    }
}

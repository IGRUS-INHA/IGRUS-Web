package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.event.domain.EventStatus;
import lombok.Getter;

/**
 * 취소할 수 없는 상태의 행사를 취소하려 할 때 발생하는 예외.
 */
@Getter
public class EventNotCancelableException extends CustomBaseException {

    private final EventStatus eventStatus;

    public EventNotCancelableException(EventStatus eventStatus) {
        super(EventErrorCode.EVENT_NOT_CANCELABLE,
                String.format("현재 상태(%s)에서는 행사를 취소할 수 없습니다.", eventStatus.getDisplayName()));
        this.eventStatus = eventStatus;
    }
}

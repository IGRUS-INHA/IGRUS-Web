package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.event.domain.EventStatus;
import lombok.Getter;

/**
 * 수정 불가능한 상태의 행사를 수정하려 할 때 발생하는 예외.
 */
@Getter
public class EventNotEditableException extends CustomBaseException {

    private final EventStatus eventStatus;

    public EventNotEditableException(EventStatus eventStatus) {
        super(EventErrorCode.EVENT_NOT_EDITABLE,
                String.format("현재 상태(%s)에서는 행사를 수정할 수 없습니다.", eventStatus.getDisplayName()));
        this.eventStatus = eventStatus;
    }

    public EventNotEditableException(EventStatus eventStatus, String message) {
        super(EventErrorCode.EVENT_NOT_EDITABLE, message);
        this.eventStatus = eventStatus;
    }
}

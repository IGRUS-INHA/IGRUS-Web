package igrus.web.event.exception;

import igrus.web.event.domain.EventStatus;

/**
 * 수정 불가능한 상태의 행사를 수정하려 할 때 발생하는 예외.
 */
public class EventNotEditableException extends RuntimeException {

    private final EventStatus status;

    public EventNotEditableException(EventStatus status) {
        super(String.format("현재 상태(%s)에서는 행사를 수정할 수 없습니다.", status.getDisplayName()));
        this.status = status;
    }

    public EventStatus getStatus() {
        return status;
    }
}

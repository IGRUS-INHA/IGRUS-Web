package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class EventNotFoundException extends CustomBaseException {

    public EventNotFoundException() {
        super(ErrorCode.EVENT_NOT_FOUND);
    }

    public EventNotFoundException(String message) {
        super(ErrorCode.EVENT_NOT_FOUND, message);
    }

    public EventNotFoundException(Long eventId) {
        super(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다: id=" + eventId);
    }
}

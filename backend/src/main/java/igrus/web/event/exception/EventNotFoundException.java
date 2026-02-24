package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

public class EventNotFoundException extends CustomBaseException {

    public EventNotFoundException() {
        super(EventErrorCode.EVENT_NOT_FOUND);
    }

    public EventNotFoundException(String message) {
        super(EventErrorCode.EVENT_NOT_FOUND, message);
    }

    public EventNotFoundException(Long eventId) {
        super(EventErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다: id=" + eventId);
    }
}

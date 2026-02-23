package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 행사가 신청 가능한 상태(OPEN)가 아닐 때 발생하는 예외.
 */
public class EventNotOpenException extends CustomBaseException {

    public EventNotOpenException() {
        super(EventErrorCode.EVENT_NOT_OPEN);
    }
}

package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 행사가 신청 가능한 상태(OPEN)가 아닐 때 발생하는 예외.
 */
public class EventNotOpenException extends CustomBaseException {

    public EventNotOpenException() {
        super(ErrorCode.EVENT_NOT_OPEN);
    }
}

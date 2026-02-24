package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 신청자가 있는 행사를 삭제하려 할 때 발생하는 예외.
 */
public class EventNotDeletableException extends CustomBaseException {

    public EventNotDeletableException() {
        super(EventErrorCode.EVENT_NOT_DELETABLE);
    }

    public EventNotDeletableException(String message) {
        super(EventErrorCode.EVENT_NOT_DELETABLE, message);
    }
}

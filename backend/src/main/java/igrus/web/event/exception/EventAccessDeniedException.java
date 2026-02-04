package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 행사 접근이 거부되었을 때 발생하는 예외.
 */
public class EventAccessDeniedException extends CustomBaseException {

    public EventAccessDeniedException() {
        super(ErrorCode.EVENT_ACCESS_DENIED);
    }

    public EventAccessDeniedException(String message) {
        super(ErrorCode.EVENT_ACCESS_DENIED, message);
    }
}

package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 행사 신청이 마감된 경우 발생하는 예외.
 */
public class EventRegistrationClosedException extends CustomBaseException {

    public EventRegistrationClosedException() {
        super(ErrorCode.EVENT_REGISTRATION_CLOSED);
    }

    public EventRegistrationClosedException(String message) {
        super(ErrorCode.EVENT_REGISTRATION_CLOSED, message);
    }
}

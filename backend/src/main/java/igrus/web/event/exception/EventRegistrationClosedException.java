package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 행사 신청이 마감된 경우 발생하는 예외.
 */
public class EventRegistrationClosedException extends CustomBaseException {

    public EventRegistrationClosedException() {
        super(EventErrorCode.EVENT_REGISTRATION_CLOSED);
    }

    public EventRegistrationClosedException(String message) {
        super(EventErrorCode.EVENT_REGISTRATION_CLOSED, message);
    }
}

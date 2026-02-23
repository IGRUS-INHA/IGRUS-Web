package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 행사 신청을 찾을 수 없는 경우 발생하는 예외.
 */
public class EventRegistrationNotFoundException extends CustomBaseException {

    public EventRegistrationNotFoundException() {
        super(EventErrorCode.EVENT_REGISTRATION_NOT_FOUND);
    }

    public EventRegistrationNotFoundException(String message) {
        super(EventErrorCode.EVENT_REGISTRATION_NOT_FOUND, message);
    }
}

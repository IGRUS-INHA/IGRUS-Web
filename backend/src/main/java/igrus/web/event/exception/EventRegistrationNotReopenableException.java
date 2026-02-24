package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 등록을 재오픈할 수 없는 상태일 때 발생하는 예외.
 */
public class EventRegistrationNotReopenableException extends CustomBaseException {

    public EventRegistrationNotReopenableException() {
        super(EventErrorCode.EVENT_REGISTRATION_NOT_REOPENABLE);
    }

    public EventRegistrationNotReopenableException(String message) {
        super(EventErrorCode.EVENT_REGISTRATION_NOT_REOPENABLE, message);
    }
}

package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 이미 행사에 신청한 경우 발생하는 예외.
 */
public class AlreadyRegisteredException extends CustomBaseException {

    public AlreadyRegisteredException() {
        super(EventErrorCode.EVENT_ALREADY_REGISTERED);
    }

    public AlreadyRegisteredException(String message) {
        super(EventErrorCode.EVENT_ALREADY_REGISTERED, message);
    }
}

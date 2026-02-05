package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 이미 행사에 신청한 경우 발생하는 예외.
 */
public class AlreadyRegisteredException extends CustomBaseException {

    public AlreadyRegisteredException() {
        super(ErrorCode.EVENT_ALREADY_REGISTERED);
    }

    public AlreadyRegisteredException(String message) {
        super(ErrorCode.EVENT_ALREADY_REGISTERED, message);
    }
}

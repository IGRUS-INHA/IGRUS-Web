package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 외부인 신청이 허용되지 않은 행사에 외부인이 신청할 때 발생하는 예외.
 */
public class ExternalRegistrationNotAllowedException extends CustomBaseException {

    public ExternalRegistrationNotAllowedException() {
        super(EventErrorCode.EXTERNAL_REGISTRATION_NOT_ALLOWED);
    }

    public ExternalRegistrationNotAllowedException(String message) {
        super(EventErrorCode.EXTERNAL_REGISTRATION_NOT_ALLOWED, message);
    }
}

package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 동일 학번 또는 전화번호로 이미 신청한 외부인이 중복 신청할 때 발생하는 예외.
 */
public class ExternalAlreadyRegisteredException extends CustomBaseException {

    public ExternalAlreadyRegisteredException() {
        super(EventErrorCode.EXTERNAL_ALREADY_REGISTERED);
    }

    public ExternalAlreadyRegisteredException(String message) {
        super(EventErrorCode.EXTERNAL_ALREADY_REGISTERED, message);
    }
}

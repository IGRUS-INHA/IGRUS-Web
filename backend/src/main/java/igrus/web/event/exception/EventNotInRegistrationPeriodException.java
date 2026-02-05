package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 신청 기간이 아닐 때 발생하는 예외.
 */
public class EventNotInRegistrationPeriodException extends CustomBaseException {

    public EventNotInRegistrationPeriodException() {
        super(ErrorCode.EVENT_NOT_IN_REGISTRATION_PERIOD);
    }
}

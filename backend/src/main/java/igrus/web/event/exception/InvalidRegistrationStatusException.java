package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 신청 상태가 유효하지 않을 때 발생하는 예외.
 * 예: 대기 중이 아닌 신청을 승인/거절하려는 경우
 */
public class InvalidRegistrationStatusException extends CustomBaseException {

    public InvalidRegistrationStatusException() {
        super(EventErrorCode.EVENT_INVALID_REGISTRATION_STATUS);
    }
}

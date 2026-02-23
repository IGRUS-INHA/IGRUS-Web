package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 잘못된 행사 날짜 설정 시 발생하는 예외.
 */
public class InvalidEventDateException extends CustomBaseException {

    public InvalidEventDateException(String message) {
        super(EventErrorCode.EVENT_INVALID_DATE, message);
    }
}

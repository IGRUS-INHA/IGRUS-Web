package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 행사 정원이 초과된 경우 발생하는 예외.
 */
public class EventCapacityFullException extends CustomBaseException {

    public EventCapacityFullException() {
        super(ErrorCode.EVENT_CAPACITY_FULL);
    }

    public EventCapacityFullException(String message) {
        super(ErrorCode.EVENT_CAPACITY_FULL, message);
    }
}

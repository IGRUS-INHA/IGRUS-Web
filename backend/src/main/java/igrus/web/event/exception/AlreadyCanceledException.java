package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 이미 취소된 신청을 다시 취소하려 할 때 발생하는 예외.
 */
public class AlreadyCanceledException extends CustomBaseException {

    public AlreadyCanceledException() {
        super(ErrorCode.EVENT_ALREADY_CANCELED);
    }
}

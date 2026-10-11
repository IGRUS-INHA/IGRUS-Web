package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 이미 취소 요청이 접수된 신청에 다시 취소 요청을 할 때 발생하는 예외.
 */
public class CancelAlreadyRequestedException extends CustomBaseException {

    public CancelAlreadyRequestedException() {
        super(EventErrorCode.EVENT_CANCEL_ALREADY_REQUESTED);
    }
}

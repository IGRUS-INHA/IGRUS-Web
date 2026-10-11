package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 처리 대기 중인 취소 요청이 없는 신청의 취소 요청을 거절하려 할 때 발생하는 예외.
 * (취소 요청한 적이 없거나, 이미 취소 처리된 신청)
 */
public class CancelRequestNotPendingException extends CustomBaseException {

    public CancelRequestNotPendingException() {
        super(EventErrorCode.EVENT_CANCEL_REQUEST_NOT_PENDING);
    }
}

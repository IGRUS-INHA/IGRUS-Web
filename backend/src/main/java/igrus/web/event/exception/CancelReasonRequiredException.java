package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 취소 요청 사유가 비어 있을 때(공백 문자만 있는 경우 포함) 발생하는 예외.
 */
public class CancelReasonRequiredException extends CustomBaseException {

    public CancelReasonRequiredException() {
        super(EventErrorCode.EVENT_CANCEL_REASON_REQUIRED);
    }
}

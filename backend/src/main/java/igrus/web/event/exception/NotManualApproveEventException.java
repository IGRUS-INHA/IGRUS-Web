package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 수동 승인(선발제) 행사가 아닌데 승인/거절하려 할 때 발생하는 예외.
 */
public class NotManualApproveEventException extends CustomBaseException {

    public NotManualApproveEventException() {
        super(EventErrorCode.EVENT_NOT_MANUAL_APPROVE);
    }
}

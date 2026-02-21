package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 운영진 이상 권한이 필요한 작업을 수행하려 할 때 발생하는 예외.
 */
public class OperatorPermissionRequiredException extends CustomBaseException {

    public OperatorPermissionRequiredException() {
        super(EventErrorCode.EVENT_OPERATOR_REQUIRED);
    }
}

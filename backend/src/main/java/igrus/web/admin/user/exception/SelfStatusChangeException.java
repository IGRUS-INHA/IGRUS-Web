package igrus.web.admin.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class SelfStatusChangeException extends CustomBaseException {

    public SelfStatusChangeException() {
        super(ErrorCode.SELF_STATUS_CHANGE_NOT_ALLOWED);
    }
}

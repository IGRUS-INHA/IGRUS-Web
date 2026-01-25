package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class LastAdminCannotChangeException extends CustomBaseException {

    public LastAdminCannotChangeException() {
        super(ErrorCode.LAST_ADMIN_CANNOT_CHANGE);
    }
}

package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class LastAdminCannotChangeException extends CustomBaseException {

    public LastAdminCannotChangeException() {
        super(AuthErrorCode.LAST_ADMIN_CANNOT_CHANGE);
    }
}

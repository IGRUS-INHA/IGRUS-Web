package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class AdminRequiredException extends CustomBaseException {

    public AdminRequiredException() {
        super(AuthErrorCode.ADMIN_REQUIRED);
    }
}

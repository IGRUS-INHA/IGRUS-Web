package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class AdminRequiredException extends CustomBaseException {

    public AdminRequiredException() {
        super(ErrorCode.ADMIN_REQUIRED);
    }
}

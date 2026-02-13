package igrus.web.admin.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class UserNotPendingVerificationException extends CustomBaseException {

    public UserNotPendingVerificationException() {
        super(ErrorCode.USER_NOT_PENDING_VERIFICATION);
    }
}

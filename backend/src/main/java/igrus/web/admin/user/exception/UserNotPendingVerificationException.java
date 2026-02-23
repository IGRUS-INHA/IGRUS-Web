package igrus.web.admin.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class UserNotPendingVerificationException extends CustomBaseException {

    public UserNotPendingVerificationException() {
        super(AdminErrorCode.USER_NOT_PENDING_VERIFICATION);
    }
}

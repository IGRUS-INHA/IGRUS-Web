package igrus.web.admin.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class SelfStatusChangeException extends CustomBaseException {

    public SelfStatusChangeException() {
        super(AdminErrorCode.SELF_STATUS_CHANGE_NOT_ALLOWED);
    }
}

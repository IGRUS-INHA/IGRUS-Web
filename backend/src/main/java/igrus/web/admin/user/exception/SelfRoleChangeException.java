package igrus.web.admin.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class SelfRoleChangeException extends CustomBaseException {

    public SelfRoleChangeException() {
        super(ErrorCode.SELF_ROLE_CHANGE_NOT_ALLOWED);
    }
}

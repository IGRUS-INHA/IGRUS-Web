package igrus.web.admin.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class SelfRoleChangeException extends CustomBaseException {

    public SelfRoleChangeException() {
        super(AdminErrorCode.SELF_ROLE_CHANGE_NOT_ALLOWED);
    }
}

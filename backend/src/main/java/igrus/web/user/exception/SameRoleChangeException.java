package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;
import igrus.web.user.domain.UserRole;

public class SameRoleChangeException extends CustomBaseException {

    public SameRoleChangeException() {
        super(ErrorCode.SAME_ROLE_CHANGE);
    }

    public SameRoleChangeException(UserRole role) {
        super(ErrorCode.SAME_ROLE_CHANGE, "이전 역할과 새 역할이 동일합니다: " + role);
    }
}
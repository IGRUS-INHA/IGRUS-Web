package igrus.web.admin.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class ForceWithdrawException extends CustomBaseException {

    public ForceWithdrawException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static ForceWithdrawException lastAdminCannotWithdraw() {
        return new ForceWithdrawException(ErrorCode.LAST_ADMIN_CANNOT_WITHDRAW);
    }
}

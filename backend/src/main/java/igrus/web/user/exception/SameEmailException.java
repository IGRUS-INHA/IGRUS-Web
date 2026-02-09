package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class SameEmailException extends CustomBaseException {

    public SameEmailException() {
        super(ErrorCode.SAME_EMAIL);
    }
}

package igrus.web.security.auth.password.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class SamePasswordException extends CustomBaseException {

    public SamePasswordException() {
        super(ErrorCode.SAME_PASSWORD);
    }
}

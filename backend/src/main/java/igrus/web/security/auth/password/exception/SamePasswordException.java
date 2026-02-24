package igrus.web.security.auth.password.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class SamePasswordException extends CustomBaseException {

    public SamePasswordException() {
        super(AuthErrorCode.SAME_PASSWORD);
    }
}

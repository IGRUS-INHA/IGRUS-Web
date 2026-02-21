package igrus.web.security.auth.password.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class InvalidPasswordFormatException extends CustomBaseException {
    public InvalidPasswordFormatException() {
        super(AuthErrorCode.INVALID_PASSWORD_FORMAT);
    }
}

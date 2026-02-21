package igrus.web.security.auth.password.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class PasswordResetTokenInvalidException extends CustomBaseException {
    public PasswordResetTokenInvalidException() {
        super(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID);
    }
}

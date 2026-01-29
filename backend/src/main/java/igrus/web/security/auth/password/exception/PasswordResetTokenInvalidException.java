package igrus.web.security.auth.password.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class PasswordResetTokenInvalidException extends CustomBaseException {
    public PasswordResetTokenInvalidException() {
        super(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
    }
}

package igrus.web.security.auth.password.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class PasswordResetTokenExpiredException extends CustomBaseException {
    public PasswordResetTokenExpiredException() {
        super(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
    }
}

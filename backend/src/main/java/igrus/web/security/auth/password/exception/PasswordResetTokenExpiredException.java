package igrus.web.security.auth.password.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class PasswordResetTokenExpiredException extends CustomBaseException {
    public PasswordResetTokenExpiredException() {
        super(AuthErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
    }
}

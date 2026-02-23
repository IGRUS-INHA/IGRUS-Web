package igrus.web.security.auth.common.exception.token;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class RefreshTokenInvalidException extends CustomBaseException {
    public RefreshTokenInvalidException() {
        super(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }
}

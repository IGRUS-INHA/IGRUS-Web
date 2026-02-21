package igrus.web.security.auth.common.exception.token;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class RefreshTokenExpiredException extends CustomBaseException {
    public RefreshTokenExpiredException() {
        super(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    }
}

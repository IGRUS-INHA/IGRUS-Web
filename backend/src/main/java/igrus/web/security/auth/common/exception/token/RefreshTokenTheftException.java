package igrus.web.security.auth.common.exception.token;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class RefreshTokenTheftException extends CustomBaseException {
    public RefreshTokenTheftException() {
        super(AuthErrorCode.REFRESH_TOKEN_THEFT_DETECTED);
    }
}

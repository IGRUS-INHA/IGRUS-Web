package igrus.web.security.auth.common.exception.token;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class RefreshTokenExpiredException extends CustomBaseException {
    public RefreshTokenExpiredException() {
        super(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }
}

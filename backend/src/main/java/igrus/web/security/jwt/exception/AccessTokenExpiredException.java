package igrus.web.security.jwt.exception;

import igrus.web.common.exception.CustomBaseException;

public class AccessTokenExpiredException extends CustomBaseException {
    public AccessTokenExpiredException() {
        super(JwtErrorCode.ACCESS_TOKEN_EXPIRED);
    }
}

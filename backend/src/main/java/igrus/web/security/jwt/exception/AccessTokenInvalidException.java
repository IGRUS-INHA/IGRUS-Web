package igrus.web.security.jwt.exception;

import igrus.web.common.exception.CustomBaseException;

public class AccessTokenInvalidException extends CustomBaseException {
    public AccessTokenInvalidException() {
        super(JwtErrorCode.ACCESS_TOKEN_INVALID);
    }
}

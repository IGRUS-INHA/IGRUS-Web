package igrus.web.security.jwt.exception;

import igrus.web.common.exception.CustomBaseException;

public class InvalidTokenTypeException extends CustomBaseException {
    public InvalidTokenTypeException() {
        super(JwtErrorCode.INVALID_TOKEN_TYPE);
    }
}

package igrus.web.security.jwt.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class AccessTokenInvalidException extends CustomBaseException {
    public AccessTokenInvalidException() {
        super(ErrorCode.ACCESS_TOKEN_INVALID);
    }
}

package igrus.web.security.jwt.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InvalidTokenTypeException extends CustomBaseException {
    public InvalidTokenTypeException() {
        super(ErrorCode.INVALID_TOKEN_TYPE);
    }
}

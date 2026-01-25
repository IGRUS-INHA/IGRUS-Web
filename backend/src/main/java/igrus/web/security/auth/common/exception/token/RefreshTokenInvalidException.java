package igrus.web.security.auth.common.exception.token;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class RefreshTokenInvalidException extends CustomBaseException {
    public RefreshTokenInvalidException() {
        super(ErrorCode.REFRESH_TOKEN_INVALID);
    }
}

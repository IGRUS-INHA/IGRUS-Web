package igrus.web.security.auth.common.exception.token;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class RefreshTokenTheftException extends CustomBaseException {
    public RefreshTokenTheftException() {
        super(ErrorCode.REFRESH_TOKEN_THEFT_DETECTED);
    }
}

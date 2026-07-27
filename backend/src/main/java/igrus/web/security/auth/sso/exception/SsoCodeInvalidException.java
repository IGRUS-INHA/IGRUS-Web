package igrus.web.security.auth.sso.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class SsoCodeInvalidException extends CustomBaseException {
    public SsoCodeInvalidException() {
        super(AuthErrorCode.SSO_CODE_INVALID);
    }
}

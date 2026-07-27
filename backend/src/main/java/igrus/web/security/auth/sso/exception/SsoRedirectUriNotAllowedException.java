package igrus.web.security.auth.sso.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class SsoRedirectUriNotAllowedException extends CustomBaseException {
    public SsoRedirectUriNotAllowedException() {
        super(AuthErrorCode.SSO_REDIRECT_URI_NOT_ALLOWED);
    }
}

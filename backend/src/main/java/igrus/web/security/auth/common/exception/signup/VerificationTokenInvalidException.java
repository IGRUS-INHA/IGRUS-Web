package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class VerificationTokenInvalidException extends CustomBaseException {
    public VerificationTokenInvalidException() {
        super(AuthErrorCode.VERIFICATION_TOKEN_INVALID);
    }
}

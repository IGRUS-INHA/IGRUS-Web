package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class VerificationTokenInvalidException extends CustomBaseException {
    public VerificationTokenInvalidException() {
        super(ErrorCode.VERIFICATION_TOKEN_INVALID);
    }
}

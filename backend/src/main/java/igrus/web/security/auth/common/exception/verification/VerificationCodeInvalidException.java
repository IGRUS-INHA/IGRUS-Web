package igrus.web.security.auth.common.exception.verification;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class VerificationCodeInvalidException extends CustomBaseException {
    public VerificationCodeInvalidException() {
        super(AuthErrorCode.VERIFICATION_CODE_INVALID);
    }
}

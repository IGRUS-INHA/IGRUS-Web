package igrus.web.security.auth.common.exception.verification;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class VerificationAttemptsExceededException extends CustomBaseException {
    public VerificationAttemptsExceededException() {
        super(AuthErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);
    }
}

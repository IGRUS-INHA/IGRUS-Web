package igrus.web.security.auth.common.exception.verification;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class VerificationAttemptsExceededException extends CustomBaseException {
    public VerificationAttemptsExceededException() {
        super(ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);
    }
}

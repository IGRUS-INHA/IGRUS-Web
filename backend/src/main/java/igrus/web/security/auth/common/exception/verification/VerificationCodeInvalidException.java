package igrus.web.security.auth.common.exception.verification;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class VerificationCodeInvalidException extends CustomBaseException {
    public VerificationCodeInvalidException() {
        super(ErrorCode.VERIFICATION_CODE_INVALID);
    }
}

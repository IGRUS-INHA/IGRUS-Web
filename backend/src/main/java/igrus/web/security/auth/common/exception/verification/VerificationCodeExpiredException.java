package igrus.web.security.auth.common.exception.verification;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class VerificationCodeExpiredException extends CustomBaseException {
    public VerificationCodeExpiredException() {
        super(ErrorCode.VERIFICATION_CODE_EXPIRED);
    }
}

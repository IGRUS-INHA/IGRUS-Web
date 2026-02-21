package igrus.web.security.auth.common.exception.verification;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class VerificationCodeExpiredException extends CustomBaseException {
    public VerificationCodeExpiredException() {
        super(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
    }
}

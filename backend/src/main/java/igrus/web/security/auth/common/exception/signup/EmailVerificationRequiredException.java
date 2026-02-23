package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class EmailVerificationRequiredException extends CustomBaseException {
    public EmailVerificationRequiredException() {
        super(AuthErrorCode.EMAIL_VERIFICATION_REQUIRED);
    }
}

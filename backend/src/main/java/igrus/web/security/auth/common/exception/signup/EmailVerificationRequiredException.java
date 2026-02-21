package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class EmailVerificationRequiredException extends CustomBaseException {
    public EmailVerificationRequiredException() {
        super(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
    }
}

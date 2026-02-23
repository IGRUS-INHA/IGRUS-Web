package igrus.web.security.auth.common.exception.email;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class EmailAlreadyVerifiedException extends CustomBaseException {
    public EmailAlreadyVerifiedException() {
        super(AuthErrorCode.EMAIL_ALREADY_VERIFIED);
    }
}
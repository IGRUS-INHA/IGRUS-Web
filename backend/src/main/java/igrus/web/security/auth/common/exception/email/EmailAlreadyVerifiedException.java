package igrus.web.security.auth.common.exception.email;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class EmailAlreadyVerifiedException extends CustomBaseException {
    public EmailAlreadyVerifiedException() {
        super(ErrorCode.EMAIL_ALREADY_VERIFIED);
    }
}
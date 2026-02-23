package igrus.web.security.auth.common.exception.email;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class EmailSendFailedException extends CustomBaseException {
    public EmailSendFailedException() {
        super(AuthErrorCode.EMAIL_SEND_FAILED);
    }
}
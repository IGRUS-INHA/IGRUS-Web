package igrus.web.security.auth.common.exception.email;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class EmailSendFailedException extends CustomBaseException {
    public EmailSendFailedException() {
        super(ErrorCode.EMAIL_SEND_FAILED);
    }
}
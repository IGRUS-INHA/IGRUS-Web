package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class EventAttachmentValidationException extends CustomBaseException {

    public EventAttachmentValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EventAttachmentValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

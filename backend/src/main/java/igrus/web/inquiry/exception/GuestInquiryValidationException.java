package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class GuestInquiryValidationException extends CustomBaseException {

    public GuestInquiryValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public GuestInquiryValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

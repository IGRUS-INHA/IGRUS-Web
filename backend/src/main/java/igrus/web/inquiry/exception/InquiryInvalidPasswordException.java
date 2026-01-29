package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InquiryInvalidPasswordException extends CustomBaseException {

    public InquiryInvalidPasswordException() {
        super(ErrorCode.INQUIRY_INVALID_PASSWORD);
    }
}

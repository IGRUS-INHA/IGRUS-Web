package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;

public class InquiryInvalidPasswordException extends CustomBaseException {

    public InquiryInvalidPasswordException() {
        super(InquiryErrorCode.INQUIRY_INVALID_PASSWORD);
    }
}

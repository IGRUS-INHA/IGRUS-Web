package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;

public class InquiryAccessDeniedException extends CustomBaseException {

    public InquiryAccessDeniedException() {
        super(InquiryErrorCode.INQUIRY_ACCESS_DENIED);
    }

    public InquiryAccessDeniedException(String message) {
        super(InquiryErrorCode.INQUIRY_ACCESS_DENIED, message);
    }
}

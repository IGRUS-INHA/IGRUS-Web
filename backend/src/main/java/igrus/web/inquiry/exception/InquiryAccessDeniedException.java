package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InquiryAccessDeniedException extends CustomBaseException {

    public InquiryAccessDeniedException() {
        super(ErrorCode.INQUIRY_ACCESS_DENIED);
    }

    public InquiryAccessDeniedException(String message) {
        super(ErrorCode.INQUIRY_ACCESS_DENIED, message);
    }
}

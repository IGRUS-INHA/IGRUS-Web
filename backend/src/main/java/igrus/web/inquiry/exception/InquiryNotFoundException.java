package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InquiryNotFoundException extends CustomBaseException {

    public InquiryNotFoundException() {
        super(ErrorCode.INQUIRY_NOT_FOUND);
    }

    public InquiryNotFoundException(Long inquiryId) {
        super(ErrorCode.INQUIRY_NOT_FOUND, "문의를 찾을 수 없습니다: id=" + inquiryId);
    }

    public InquiryNotFoundException(String inquiryNumber) {
        super(ErrorCode.INQUIRY_NOT_FOUND, "문의를 찾을 수 없습니다: number=" + inquiryNumber);
    }
}

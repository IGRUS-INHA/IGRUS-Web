package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;

public class InquiryNotFoundException extends CustomBaseException {

    public InquiryNotFoundException() {
        super(InquiryErrorCode.INQUIRY_NOT_FOUND);
    }

    public InquiryNotFoundException(Long inquiryId) {
        super(InquiryErrorCode.INQUIRY_NOT_FOUND, "문의를 찾을 수 없습니다: id=" + inquiryId);
    }

    public InquiryNotFoundException(String inquiryNumber) {
        super(InquiryErrorCode.INQUIRY_NOT_FOUND, "문의를 찾을 수 없습니다: number=" + inquiryNumber);
    }
}

package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;

public class InquiryAlreadyRepliedException extends CustomBaseException {

    public InquiryAlreadyRepliedException() {
        super(InquiryErrorCode.INQUIRY_ALREADY_REPLIED);
    }

    public InquiryAlreadyRepliedException(Long inquiryId) {
        super(InquiryErrorCode.INQUIRY_ALREADY_REPLIED, "이미 답변이 작성된 문의입니다: id=" + inquiryId);
    }
}

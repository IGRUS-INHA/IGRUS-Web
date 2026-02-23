package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;

public class InquiryReplyNotFoundException extends CustomBaseException {

    public InquiryReplyNotFoundException() {
        super(InquiryErrorCode.INQUIRY_REPLY_NOT_FOUND);
    }

    public InquiryReplyNotFoundException(Long inquiryId) {
        super(InquiryErrorCode.INQUIRY_REPLY_NOT_FOUND, "문의 ID " + inquiryId + "에 대한 답변을 찾을 수 없습니다");
    }
}

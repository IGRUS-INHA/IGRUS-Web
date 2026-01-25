package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InquiryReplyNotFoundException extends CustomBaseException {

    public InquiryReplyNotFoundException() {
        super(ErrorCode.INQUIRY_REPLY_NOT_FOUND);
    }

    public InquiryReplyNotFoundException(Long inquiryId) {
        super(ErrorCode.INQUIRY_REPLY_NOT_FOUND, "문의 ID " + inquiryId + "에 대한 답변을 찾을 수 없습니다");
    }
}

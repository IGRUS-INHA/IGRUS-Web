package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InquiryAlreadyRepliedException extends CustomBaseException {

    public InquiryAlreadyRepliedException() {
        super(ErrorCode.INQUIRY_ALREADY_REPLIED);
    }

    public InquiryAlreadyRepliedException(Long inquiryId) {
        super(ErrorCode.INQUIRY_ALREADY_REPLIED, "이미 답변이 작성된 문의입니다: id=" + inquiryId);
    }
}

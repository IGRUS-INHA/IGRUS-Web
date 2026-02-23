package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;

public class InquiryMaxAttachmentsExceededException extends CustomBaseException {

    public InquiryMaxAttachmentsExceededException() {
        super(InquiryErrorCode.INQUIRY_MAX_ATTACHMENTS_EXCEEDED);
    }

    public InquiryMaxAttachmentsExceededException(int count) {
        super(InquiryErrorCode.INQUIRY_MAX_ATTACHMENTS_EXCEEDED, "첨부파일은 최대 3개까지 가능합니다. 현재: " + count + "개");
    }
}

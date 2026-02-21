package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;

public class InquiryNumberGenerationException extends CustomBaseException {

    public InquiryNumberGenerationException() {
        super(InquiryErrorCode.INQUIRY_NUMBER_GENERATION_FAILED);
    }

    public InquiryNumberGenerationException(Throwable cause) {
        super(InquiryErrorCode.INQUIRY_NUMBER_GENERATION_FAILED, cause);
    }
}

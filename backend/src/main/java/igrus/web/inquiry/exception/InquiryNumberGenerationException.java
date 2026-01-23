package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InquiryNumberGenerationException extends CustomBaseException {

    public InquiryNumberGenerationException() {
        super(ErrorCode.INQUIRY_NUMBER_GENERATION_FAILED);
    }

    public InquiryNumberGenerationException(Throwable cause) {
        super(ErrorCode.INQUIRY_NUMBER_GENERATION_FAILED, cause);
    }
}

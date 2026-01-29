package igrus.web.inquiry.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;
import igrus.web.inquiry.domain.InquiryStatus;

public class InvalidStatusTransitionException extends CustomBaseException {

    public InvalidStatusTransitionException() {
        super(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    public InvalidStatusTransitionException(InquiryStatus currentStatus, InquiryStatus newStatus) {
        super(ErrorCode.INVALID_STATUS_TRANSITION,
                String.format("상태 변경이 허용되지 않습니다: %s → %s", currentStatus.getDescription(), newStatus.getDescription()));
    }
}

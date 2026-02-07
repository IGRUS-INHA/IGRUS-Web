package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class BulkRejectionEmptyException extends CustomBaseException {

    public BulkRejectionEmptyException() {
        super(ErrorCode.BULK_REJECTION_EMPTY);
    }
}

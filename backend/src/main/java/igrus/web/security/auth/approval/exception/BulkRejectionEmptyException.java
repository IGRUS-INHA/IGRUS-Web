package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class BulkRejectionEmptyException extends CustomBaseException {

    public BulkRejectionEmptyException() {
        super(AuthErrorCode.BULK_REJECTION_EMPTY);
    }
}

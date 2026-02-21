package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class BulkApprovalEmptyException extends CustomBaseException {

    public BulkApprovalEmptyException() {
        super(AuthErrorCode.BULK_APPROVAL_EMPTY);
    }
}

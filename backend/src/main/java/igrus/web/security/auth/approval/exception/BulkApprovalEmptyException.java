package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class BulkApprovalEmptyException extends CustomBaseException {

    public BulkApprovalEmptyException() {
        super(ErrorCode.BULK_APPROVAL_EMPTY);
    }
}

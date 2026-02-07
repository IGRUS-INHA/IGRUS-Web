package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class AssociateAlreadyDecidedException extends CustomBaseException {

    public AssociateAlreadyDecidedException() {
        super(ErrorCode.ASSOCIATE_ALREADY_DECIDED);
    }
}

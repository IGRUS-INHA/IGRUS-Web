package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class AssociateAlreadyDecidedException extends CustomBaseException {

    public AssociateAlreadyDecidedException() {
        super(AuthErrorCode.ASSOCIATE_ALREADY_DECIDED);
    }
}

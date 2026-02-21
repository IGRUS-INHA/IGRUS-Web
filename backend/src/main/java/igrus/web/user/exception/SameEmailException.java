package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class SameEmailException extends CustomBaseException {

    public SameEmailException() {
        super(UserErrorCode.SAME_EMAIL);
    }
}

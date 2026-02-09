package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class SamePhoneNumberException extends CustomBaseException {

    public SamePhoneNumberException() {
        super(ErrorCode.SAME_PHONE_NUMBER);
    }
}

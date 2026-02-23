package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class SamePhoneNumberException extends CustomBaseException {

    public SamePhoneNumberException() {
        super(UserErrorCode.SAME_PHONE_NUMBER);
    }
}

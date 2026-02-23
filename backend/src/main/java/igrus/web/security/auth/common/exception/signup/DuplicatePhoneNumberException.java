package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.user.exception.UserErrorCode;

public class DuplicatePhoneNumberException extends CustomBaseException {
    public DuplicatePhoneNumberException() {
        super(UserErrorCode.DUPLICATE_PHONE_NUMBER);
    }
}

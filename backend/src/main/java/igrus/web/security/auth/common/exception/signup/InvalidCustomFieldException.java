package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class InvalidCustomFieldException extends CustomBaseException {
    public InvalidCustomFieldException() {
        super(AuthErrorCode.INVALID_CUSTOM_FIELD);
    }
}

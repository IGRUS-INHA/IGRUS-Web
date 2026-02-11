package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InvalidCustomFieldException extends CustomBaseException {
    public InvalidCustomFieldException() {
        super(ErrorCode.INVALID_CUSTOM_FIELD);
    }
}

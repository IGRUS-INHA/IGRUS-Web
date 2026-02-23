package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class DuplicateStudentIdException extends CustomBaseException {
    public DuplicateStudentIdException() {
        super(AuthErrorCode.DUPLICATE_STUDENT_ID);
    }
}

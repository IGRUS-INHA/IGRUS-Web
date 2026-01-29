package igrus.web.security.auth.common.exception.signup;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class DuplicateStudentIdException extends CustomBaseException {
    public DuplicateStudentIdException() {
        super(ErrorCode.DUPLICATE_STUDENT_ID);
    }
}

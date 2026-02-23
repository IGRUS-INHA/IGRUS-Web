package igrus.web.user.semester.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.user.exception.UserErrorCode;

public class InvalidSemesterException extends CustomBaseException {

    public InvalidSemesterException() {
        super(UserErrorCode.SEMESTER_INVALID_SEMESTER);
    }

    public InvalidSemesterException(String message) {
        super(UserErrorCode.SEMESTER_INVALID_SEMESTER, message);
    }
}

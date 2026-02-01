package igrus.web.user.semester.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InvalidSemesterException extends CustomBaseException {

    public InvalidSemesterException() {
        super(ErrorCode.SEMESTER_INVALID_SEMESTER);
    }

    public InvalidSemesterException(String message) {
        super(ErrorCode.SEMESTER_INVALID_SEMESTER, message);
    }
}

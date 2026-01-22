package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InvalidStudentIdException extends CustomBaseException {

    public InvalidStudentIdException() {
        super(ErrorCode.INVALID_STUDENT_ID);
    }

    public InvalidStudentIdException(String studentId) {
        super(ErrorCode.INVALID_STUDENT_ID, "학번은 8자리 숫자여야 합니다: " + studentId);
    }
}
